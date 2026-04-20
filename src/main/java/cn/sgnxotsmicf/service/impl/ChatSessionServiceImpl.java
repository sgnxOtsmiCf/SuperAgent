package cn.sgnxotsmicf.service.impl;

import cn.sgnxotsmicf.chatMemory.MySqlChatMemoryRepository;
import cn.sgnxotsmicf.common.po.ChatSession;
import cn.sgnxotsmicf.common.result.Result;
import cn.sgnxotsmicf.common.result.ResultCodeEnum;
import cn.sgnxotsmicf.common.tools.ServiceUtil;
import cn.sgnxotsmicf.common.tools.SessionIdUtil;
import cn.sgnxotsmicf.common.vo.ChatMessageVo;
import cn.sgnxotsmicf.common.vo.ChatSessionVo;
import cn.sgnxotsmicf.dao.ChatSessionMapper;
import cn.sgnxotsmicf.service.ChatSessionService;
import cn.sgnxotsmicf.service.SessionArchiveService;
import cn.sgnxotsmicf.service.strategy.ChatSessionContext;
import cn.sgnxotsmicf.service.strategy.ChatSessionStrategy;
import cn.sgnxotsmicf.service.strategy.ChatSessionStrategyFactory;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/6 11:21
 * @Version: 1.0
 * @Description:
 */

@Service
@Slf4j
public class ChatSessionServiceImpl extends ServiceImpl<ChatSessionMapper, ChatSession> implements ChatSessionService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private MySqlChatMemoryRepository mysqlChatMemoryRepository;

    @Resource
    private ServiceUtil serviceUtil;

    @Resource
    private SessionArchiveService sessionArchiveService;

    @Resource
    private ChatSessionStrategyFactory chatSessionStrategyFactory;

    @Override
    public Result<List<ChatSessionVo>> getSessions(Long agentId) {
        long userId = serviceUtil.getUserId();
        ChatSessionStrategy strategy = chatSessionStrategyFactory.getStrategy(agentId);
        ChatSessionContext chatSessionContext = ChatSessionContext.builder().chatSessionService(this).agentId(agentId).userId(userId).build();
        List<ChatSessionVo> chatSessionVoList = strategy.getSessions(chatSessionContext);
        if (chatSessionVoList == null || chatSessionVoList.isEmpty()) {
            return Result.build(ResultCodeEnum.AGENT_ID_NO_EXIST);
        }
        return Result.ok(chatSessionVoList);
    }

    @Override
    public Result<List<ChatSessionVo>> getSessionsPage(Long agentId, Integer pageNo, Integer pageSize) {
        long userId = serviceUtil.getUserId();
        ChatSessionStrategy strategy = chatSessionStrategyFactory.getStrategy(agentId);
        ChatSessionContext chatSessionContext = ChatSessionContext.builder()
                .chatSessionService(this).agentId(agentId).userId(userId).pageNo(pageNo).pageSize(pageSize).build();
        List<ChatSessionVo> chatSessionVoList = strategy.getSessionsPage(chatSessionContext);
        if (chatSessionVoList == null || chatSessionVoList.isEmpty()) {
            return Result.build(ResultCodeEnum.AGENT_ID_NO_EXIST);
        }
        return Result.ok(chatSessionVoList);
    }

    @Override
    public Result<ChatSessionVo> getSessionBySessionId(Long agentId, String sessionId) {
        long userId = serviceUtil.getUserId();
        ChatSessionStrategy strategy = chatSessionStrategyFactory.getStrategy(agentId);
        ChatSessionContext chatSessionContext = ChatSessionContext.builder()
                .chatSessionService(this).agentId(agentId).sessionId(sessionId).userId(userId).build();
        ChatSessionVo chatSessionVo = strategy.getSessionBySessionId(chatSessionContext);
        if (chatSessionVo == null) {
            return Result.build(ResultCodeEnum.AGENT_ID_NO_EXIST);
        }
        return Result.ok(chatSessionVo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<String> deleteSessionById(Long agentId, String sessionId) {
        Long userId = serviceUtil.getUserId();
        SessionIdUtil.ValidationResult validationResult = SessionIdUtil.validateSession(sessionId);
        if (validationResult.isExpired()){
            return Result.build(ResultCodeEnum.SESSION_ID_EXPIRED);
        }else if (!validationResult.isValid()){
            return Result.build(ResultCodeEnum.SESSION_ID_NO_EXITS);
        }
        boolean flag = remove(new LambdaQueryWrapper<ChatSession>()
                .eq(ChatSession::getSessionId, sessionId)
                .eq(ChatSession::getAgentId, agentId)
                .eq(ChatSession::getUserId, userId)
                .eq(ChatSession::getIsDeleted, 0)
        );
        if (!flag) {
            return Result.build(ResultCodeEnum.SESSION_ID_NO_EXITS);
        }
        String redisKeyPrefix = SessionIdUtil.getRedisKeyPrefixBySessionId(sessionId);
        stringRedisTemplate.delete(redisKeyPrefix + sessionId);
        stringRedisTemplate.opsForZSet().remove(redisKeyPrefix + userId.toString(), sessionId);

        ChatSessionStrategy strategy = chatSessionStrategyFactory.getStrategy(agentId);
        ChatSessionContext chatSessionContext = ChatSessionContext.builder().agentId(agentId).sessionId(sessionId).userId(userId).build();
        boolean status = strategy.deleteMemorySessionById(chatSessionContext);
        if (!status) {
            return Result.build(ResultCodeEnum.AGENT_ID_NO_EXIST);
        }
        return Result.ok();

    }

    @Override
    public Result<String> setNameBySessionId(Long agentId, String sessionId, String sessionName) {
        SessionIdUtil.ValidationResult validationResult = SessionIdUtil.validateSession(sessionId);
        if (validationResult.isExpired()){
            return Result.build(ResultCodeEnum.SESSION_ID_EXPIRED);
        }else if (!validationResult.isValid()){
            return Result.build(ResultCodeEnum.SESSION_ID_NO_EXITS);
        }
        ChatSession chatSession = getOne(new LambdaQueryWrapper<ChatSession>().eq(ChatSession::getSessionId, sessionId).select(ChatSession::getSessionStatus));
        if (chatSession == null) {
            return Result.build(ResultCodeEnum.AGENT_ID_NO_EXIST);
        }
        Integer sessionStatus = chatSession.getSessionStatus();
        if (sessionStatus == 0){
            serviceUtil.updateSessionSessionNameArchive(sessionId, sessionName, this);
        }
        serviceUtil.updateSessionSessionName(sessionId, sessionName, this, false);
        return Result.ok();
    }

    @Override
    public Result<String> setTopSession(Long agentId, String sessionId) {
        Long userId = serviceUtil.getUserId();
        SessionIdUtil.ValidationResult validationResult = SessionIdUtil.validateSession(sessionId, agentId);
        if (validationResult.isExpired()){
            Result<String> stringResult = sessionArchiveService.sendBatchArchiveMessagesBySessionIdList(List.of(sessionId), userId, agentId);
            log.info(stringResult.getMessage());
            return Result.build(ResultCodeEnum.SESSION_ID_EXPIRED);
        }else if (!validationResult.isValid()){
            return Result.build(ResultCodeEnum.SESSION_ID_NO_EXITS);
        }
        serviceUtil.setIsTopSession(sessionId, true);
        return Result.ok();
    }

    @Override
    public Result<String> setUpTopSession(Long agentId, String sessionId) {
        Long userId = serviceUtil.getUserId();
        SessionIdUtil.ValidationResult validationResult = SessionIdUtil.validateSession(sessionId, agentId);
        if (validationResult.isExpired()){
            Result<String> stringResult = sessionArchiveService.sendBatchArchiveMessagesBySessionIdList(List.of(sessionId), userId, agentId);
            log.info(stringResult.getMessage());
            return Result.build(ResultCodeEnum.SESSION_ID_EXPIRED);
        }else if (!validationResult.isValid()){
            return Result.build(ResultCodeEnum.SESSION_ID_NO_EXITS);
        }
        serviceUtil.setIsTopSession(sessionId, false);
        return Result.ok();
    }

    /**
     * mySql级别的将List<String> sessionIdList 转变为List<ChatSessionVo>
     *
     * @param sessionIdList 会话id列表
     * @return List<ChatSessionVo>对象
     */
    public List<ChatSessionVo> getChatSessionVoListBySessionIds(List<String> sessionIdList) {
        SessionIdUtil.BatchValidationResult batchValidationResult = SessionIdUtil.validateBatch(sessionIdList);
        sessionIdList = new ArrayList<>(batchValidationResult.getValidIds());
        List<ChatSessionVo> chatSessionVoList = new ArrayList<>();
        for (String sessionId : sessionIdList) {
            ChatSession chatSession = getOne(new LambdaQueryWrapper<ChatSession>().eq(ChatSession::getSessionId, sessionId));
            List<ChatMessageVo> chatMessageVoList = mysqlChatMemoryRepository.findByConversationIdToChatMessage(sessionId).stream().map(chatMessage -> {
                ChatMessageVo chatMessageVo = new ChatMessageVo();
                BeanUtils.copyProperties(chatMessage, chatMessageVo);
                return chatMessageVo;
            }).collect(Collectors.toList());
            ChatSessionVo chatSessionVo = new ChatSessionVo();
            BeanUtils.copyProperties(chatSession, chatSessionVo);
            chatSessionVo.setContent(chatMessageVoList);
            chatSessionVoList.add(chatSessionVo);
        }
        return chatSessionVoList;
    }

    @Override
    public List<ChatSessionVo> getChatSessionVoListBySessionIdMessageListMap(Map<String, List<Message>> sessionIdMessageListMap, Set<String> filterSessionIdList) {
        return sessionIdMessageListMap.entrySet().stream().filter(entry -> !filterSessionIdList.contains(entry.getKey())).map(entry -> {
                    String sessionId = entry.getKey();
                    List<Message> messageList = entry.getValue();
                    List<ChatMessageVo> chatMessageVoList = messageList.stream().map(message -> serviceUtil.getChatMessageVoByMessage(message)).collect(Collectors.toList());
                    return serviceUtil.getChatSessionVo(sessionId, chatMessageVoList, this);
                }).filter(Objects::nonNull)
                .sorted((vo1, vo2) -> vo2.getLastActive().compareTo(vo1.getLastActive()))
                .collect(Collectors.toList());
    }

    @Override
    public List<ChatSessionVo> getChatSessionVoListBySessionIdMessageListMap(Map<String, List<Message>> sessionIdMessageListMap) {
        return sessionIdMessageListMap.entrySet().stream().map(entry -> {
                    String sessionId = entry.getKey();
                    List<Message> messageList = entry.getValue();
                    List<ChatMessageVo> chatMessageVoList = messageList.stream().map(message -> serviceUtil.getChatMessageVoByMessage(message)).collect(Collectors.toList());
                    return serviceUtil.getChatSessionVo(sessionId, chatMessageVoList, this);
                }).filter(Objects::nonNull)
                .sorted((vo1, vo2) -> vo2.getLastActive().compareTo(vo1.getLastActive()))
                .collect(Collectors.toList());
    }
}
