package cn.sgnxotsmicf.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.sgnxotsmicf.app.superagent.SuperAgentFactory;
import cn.sgnxotsmicf.chatMemory.NoSqlChatMemoryFactory;
import cn.sgnxotsmicf.common.rabbitmq.entity.ArchiveMessage;
import cn.sgnxotsmicf.common.po.ChatMessage;
import cn.sgnxotsmicf.common.po.ChatSession;
import cn.sgnxotsmicf.common.rabbitmq.constant.MqConst;
import cn.sgnxotsmicf.common.rabbitmq.service.RabbitService;
import cn.sgnxotsmicf.common.result.Result;
import cn.sgnxotsmicf.common.result.ResultCodeEnum;
import cn.sgnxotsmicf.common.tools.AgentCommon;
import cn.sgnxotsmicf.common.tools.ServiceUtil;
import cn.sgnxotsmicf.common.tools.SessionIdUtil;
import cn.sgnxotsmicf.common.vo.ChatMessageVo;
import cn.sgnxotsmicf.common.vo.ChatSessionVo;
import cn.sgnxotsmicf.dao.ChatSessionMapper;
import cn.sgnxotsmicf.service.ChatMessageService;
import cn.sgnxotsmicf.service.SessionArchiveService;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.checkpoint.Checkpoint;
import com.alibaba.cloud.ai.graph.checkpoint.savers.redis.RedisSaver;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.config.Config;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 会话归档服务实现
 * <p>
 * 使用RedisSaver读取Spring AI Alibaba的Redis数据
 * 简化设计：Message直接转PO，无需中间转换
 */
@Slf4j
@Service
public class SessionArchiveServiceImpl implements SessionArchiveService {

    @Resource
    private RabbitService rabbitService;

    @Resource
    private ChatSessionMapper chatSessionMapper;

    @Resource
    private ChatMessageService chatMessageService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private ServiceUtil serviceUtil;

    @Resource
    private SuperAgentFactory superAgentFactory;

    @Resource
    private NoSqlChatMemoryFactory noSqlChatMemoryFactory;

    @Resource
    private Config config;

    // ==================== 消息发送（生产者）====================

    @Override
    public Result<String> sendArchiveMessage(String sessionId, Long agentId) {
        SessionIdUtil.ValidationResult validationResult = SessionIdUtil.validateSession(sessionId, agentId);
        if (!validationResult.isValid()) {
            return Result.build(ResultCodeEnum.SESSION_ID_NO_EXITS);
        }
        Long userId = serviceUtil.getUserId();
        ArchiveMessage message = ArchiveMessage.createManualArchiveMessage(sessionId, userId, agentId);
        boolean success = rabbitService.sendMessage(
                MqConst.EXCHANGE_ARCHIVE,
                MqConst.ROUTING_ARCHIVE_SESSION,
                message
        );
        return success ? Result.ok("归档请求已提交") : Result.build(ResultCodeEnum.SERVICE_ERROR);
    }

    @Override
    public Result<String> sendAutoArchiveMessage(String sessionId, Long userId, Long agentId) {
        ArchiveMessage message = ArchiveMessage.createAutoArchiveMessage(sessionId, userId, agentId);
        boolean success = rabbitService.sendMessage(
                MqConst.EXCHANGE_ARCHIVE,
                MqConst.ROUTING_ARCHIVE_SESSION,
                message
        );
        return success ? Result.ok() : Result.build(ResultCodeEnum.SERVICE_ERROR);
    }

    @Override
    public Result<String> sendBatchArchiveMessages(List<ArchiveMessage> messages) {
        int successCount = 0;
        for (ArchiveMessage message : messages) {
            boolean success = rabbitService.sendMessage(
                    MqConst.EXCHANGE_ARCHIVE,
                    MqConst.ROUTING_ARCHIVE_SESSION,
                    message
            );
            if (success) successCount++;
        }
        log.info("批量归档消息发送完成: 成功={}", successCount);
        return Result.ok("批量归档请求已提交: 成功" + successCount + "条");
    }

    @Override
    public Result<String> sendBatchArchiveMessagesBySessionIdList(Collection<String> sessionList, Long userId, Long agentId) {
        int successCount = 0;
        for (String sessionId : sessionList) {
            ArchiveMessage message = ArchiveMessage.createAutoArchiveMessage(sessionId, userId, agentId);
            boolean success = rabbitService.sendMessage(
                    MqConst.EXCHANGE_ARCHIVE,
                    MqConst.ROUTING_ARCHIVE_SESSION,
                    message
            );
            if (success) successCount++;
        }
        log.info("批量归档消息发送完成: 成功={}", successCount);
        return Result.ok("批量归档请求已提交: 成功" + successCount + "条");
    }

    // ==================== 消息消费（业务逻辑）====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean processArchiveMessage(ArchiveMessage message) {
        String sessionId = message.getSessionId();
        Long agentId = message.getAgentId();
        Long userId = message.getUserId();
        try {
            // 1. 从Redis读取会话数据（使用RedisSaver）
            List<Message> messages = readMessagesFromRedis(sessionId, agentId, userId);

            if (messages.isEmpty()) {
                log.warn("Redis中未找到会话消息: sessionId={}", sessionId);
                markSessionAsArchived(sessionId);
                return true;
            }

            log.info("从Redis读取到 {} 条消息: sessionId={}", messages.size(), sessionId);

            // 2. Message直接转PO，保存到MySQL
            boolean flag = saveMessagesToMySQL(agentId, sessionId, messages);
            if (!flag) {
                return false;
            }
            // 3. 更新会话状态为归档
            markSessionAsArchived(sessionId);

            // 4. 删除Redis数据
            deleteRedisData(sessionId, agentId, userId);

            log.info("归档完成: sessionId={}, messageCount={}", sessionId, messages.size());
            return true;

        } catch (Exception e) {
            log.error("归档处理失败: sessionId={}, error={}", sessionId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 使用RedisSaver从Redis读取消息
     */
    private List<Message> readMessagesFromRedis(String sessionId, Long agentId, Long userId) {
        List<Message> messages = new ArrayList<>();
        try {
            if (AgentCommon.SuperAgentId.equals(agentId)) {
                // SuperAgent使用RedisSaver
                RedisSaver redisSaver = superAgentFactory.buildRedisSaver();
                RunnableConfig runnableConfig = RunnableConfig.builder()
                        .threadId(sessionId)
                        .build();
                Optional<Checkpoint> checkpoint = redisSaver.get(runnableConfig);

                if (checkpoint.isPresent()) {
                    ArrayList<Message> msgList =
                            (ArrayList<Message>) checkpoint.get().getState().get("messages");
                    if (msgList != null) {
                        messages = msgList;
                    }
                }
            } else {
                // 非SuperAgent会话，从Redis直接读取
                String redisKey = SessionIdUtil.getRedisKeyPrefixBySessionId(sessionId);

                if (redisKey != null) {
                    ChatMemoryRepository redisChatMemoryRepository = noSqlChatMemoryFactory.getRedissonRedisChatMemoryRepository(config, userId, agentId);
                    return redisChatMemoryRepository.findByConversationId(sessionId);
                }
            }
        } catch (Exception e) {
            log.error("从Redis读取消息失败: sessionId={}, error={}", sessionId, e.getMessage());
        }

        return messages;
    }

    /**
     * Message直接转PO，保存到MySQL
     * 简化设计：直接使用Message对象转换
     */
    private boolean saveMessagesToMySQL(Long agentId, String sessionId, List<Message> messages) {
        List<ChatMessage> chatMessageList = serviceUtil.convertToChatMessage(messages, sessionId);
        if (CollUtil.isNotEmpty(chatMessageList)) {
            chatMessageService.saveBatch(chatMessageList);
            return true;
        }
        return false;
    }

    /**
     * 将会话标记为已归档
     */
    private void markSessionAsArchived(String sessionId) {
        LambdaUpdateWrapper<ChatSession> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(ChatSession::getSessionId, sessionId)
                .eq(ChatSession::getIsDeleted, 0)
                .set(ChatSession::getSessionStatus, 0)
                .set(ChatSession::getLastActive, LocalDateTime.now());
        chatSessionMapper.update(null, updateWrapper);
    }

    /**
     * 删除Redis中的会话数据
     */
    private void deleteRedisData(String sessionId, Long agentId, Long userId) {
        try {
            if (AgentCommon.SuperAgentId.equals(agentId)) {
                RedisSaver redisSaver = superAgentFactory.buildRedisSaver();
                redisSaver.release(RunnableConfig.builder().threadId(sessionId).build());
            } else if (AgentCommon.OpenManusId.equals(agentId) || AgentCommon.FamilyHarmonyId.equals(agentId)) {
                ChatMemoryRepository redisChatMemoryRepository = noSqlChatMemoryFactory.getRedissonRedisChatMemoryRepository(config, userId, agentId);
                redisChatMemoryRepository.deleteByConversationId(sessionId);
            }
            String redisKey = SessionIdUtil.getRedisKeyPrefixBySessionId(sessionId);
            if (redisKey != null) {
                stringRedisTemplate.delete(redisKey + sessionId);
            }

            log.info("Redis数据已删除: sessionId={}", sessionId);
        } catch (Exception e) {
            log.error("删除Redis数据失败: sessionId={}, error={}", sessionId, e.getMessage());
        }
    }

    // ==================== 查询方法 ====================

    @Override
    public boolean isSessionArchived(String sessionId) {
        SessionIdUtil.ValidationResult validationResult = SessionIdUtil.validateSession(sessionId);
        if (!validationResult.isValid()) {
            return false;
        }
        LambdaQueryWrapper<ChatSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatSession::getSessionId, sessionId)
                .eq(ChatSession::getSessionStatus, 0);
        return chatSessionMapper.selectCount(wrapper) > 0;
    }

    @Override
    public boolean validateSessionOwnership(String sessionId, Long userId) {
        LambdaQueryWrapper<ChatSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatSession::getSessionId, sessionId)
                .eq(ChatSession::getUserId, userId);
        return chatSessionMapper.selectCount(wrapper) > 0;
    }

    @Override
    public List<String> getExpiredSessions() {
        LambdaQueryWrapper<ChatSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatSession::getSessionStatus, 1)
                .eq(ChatSession::getIsDeleted, 0);

        List<ChatSession> activeSessions = chatSessionMapper.selectList(wrapper);

        return activeSessions.stream()
                .map(ChatSession::getSessionId)
                .filter(sessionId -> !SessionIdUtil.isValid(sessionId))
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getExpiringSessions(int days) {
        LambdaQueryWrapper<ChatSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatSession::getSessionStatus, 1)
                .eq(ChatSession::getIsDeleted, 0);

        List<ChatSession> activeSessions = chatSessionMapper.selectList(wrapper);

        return activeSessions.stream()
                .map(ChatSession::getSessionId)
                .filter(sessionId -> {
                    long remainingTime = SessionIdUtil.getRemainingTime(sessionId);
                    return remainingTime > 0 && remainingTime <= (long) days * 24 * 60 * 60;
                })
                .collect(Collectors.toList());
    }

    /**
     * 这个方法归档专用，其他查询误调用
     *
     * @param agentId  顾名思义
     * @param pageNo   页码
     * @param pageSize 每页尺寸
     * @return 结果集
     */
    @Override
    public List<ChatSessionVo> getArchiveSessionPage(Long agentId, Integer pageNo, Integer pageSize) {
        Long userId = serviceUtil.getUserId();
        IPage<ChatSession> page = new Page<>(pageNo, pageSize);
        LambdaQueryWrapper<ChatSession> chatSessionLambdaQueryWrapper = new LambdaQueryWrapper<ChatSession>()
                .eq(ChatSession::getAgentId, agentId)
                .eq(ChatSession::getUserId, userId)
                .eq(ChatSession::getSessionStatus, 0)
                .eq(ChatSession::getIsDeleted, 0)
                .orderByDesc(ChatSession::getLastActive);
        //chatSessionService.list(page, chatSessionLambdaQueryWrapper);
        chatSessionMapper.selectPage(page, chatSessionLambdaQueryWrapper);
        List<String> sessionIdList = page.getRecords().stream().map(ChatSession::getSessionId).collect(Collectors.toList());
        if (CollUtil.isEmpty(sessionIdList)) {
            return Collections.emptyList();
        }
        Map<String, List<ChatMessage>> sessionIdListMap = chatMessageService.list(
                new LambdaQueryWrapper<ChatMessage>().in(ChatMessage::getSessionId, sessionIdList)
        ).stream().collect(Collectors.groupingBy(ChatMessage::getSessionId));
        return page.getRecords().stream().map(chatSession -> {
            ChatSessionVo chatSessionVo = new ChatSessionVo();
            BeanUtil.copyProperties(chatSession, chatSessionVo);
            List<ChatMessageVo> chatMessageVoList = Optional.ofNullable(sessionIdListMap.get(chatSession.getSessionId()))
                    .orElseGet(Collections::emptyList).stream().map(chatMessage -> {
                        ChatMessageVo chatMessageVo = new ChatMessageVo();
                        BeanUtil.copyProperties(chatMessage, chatMessageVo);
                        return chatMessageVo;
                    }).collect(Collectors.toList());
            chatSessionVo.setContent(chatMessageVoList);
            return chatSessionVo;
        }).collect(Collectors.toList());
    }
}
