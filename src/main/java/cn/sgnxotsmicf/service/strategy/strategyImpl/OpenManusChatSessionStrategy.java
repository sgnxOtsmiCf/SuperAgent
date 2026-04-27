package cn.sgnxotsmicf.service.strategy.strategyImpl;

import cn.sgnxotsmicf.chatMemory.CustomRedissonRedisChatMemoryRepository;
import cn.sgnxotsmicf.chatMemory.MySqlChatMemoryRepository;
import cn.sgnxotsmicf.common.result.Result;
import cn.sgnxotsmicf.common.result.ResultCodeEnum;
import cn.sgnxotsmicf.common.agent.AgentCommon;
import cn.sgnxotsmicf.common.tools.ServiceUtil;
import cn.sgnxotsmicf.common.tools.SessionIdUtil;
import cn.sgnxotsmicf.common.vo.ChatMessageVo;
import cn.sgnxotsmicf.common.vo.ChatSessionVo;
import cn.sgnxotsmicf.exception.AgentException;
import cn.sgnxotsmicf.service.ChatSessionService;
import cn.sgnxotsmicf.service.SessionArchiveService;
import cn.sgnxotsmicf.service.strategy.ChatSessionContext;
import cn.sgnxotsmicf.service.strategy.ChatSessionStrategy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/17 12:05
 * @Version: 1.0
 * @Description:
 */

@Slf4j
@Component(AgentCommon.OpenManus)
public class OpenManusChatSessionStrategy implements ChatSessionStrategy {

    @Resource
    private ServiceUtil serviceUtil;

    @Resource
    private SessionArchiveService sessionArchiveService;

    @Resource
    private MySqlChatMemoryRepository mySqlChatMemoryRepository;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean supports(Long agentId) {
        return AgentCommon.OpenManusId.equals(agentId) || AgentCommon.FamilyHarmonyId.equals(agentId);
    }

    @Override
    public List<ChatSessionVo> getSessions(ChatSessionContext chatSessionContext) {
        Long agentId = chatSessionContext.getAgentId();
        Long userId = chatSessionContext.getUserId();
        ChatSessionService chatSessionService = chatSessionContext.getChatSessionService();

        ChatMemoryRepository redisChatMemoryRepository = serviceUtil.getRedissonRedisChatMemoryRepository(userId, agentId);
        Map<String, List<Message>> sessionIdMessageListMap = null;
        if (redisChatMemoryRepository instanceof CustomRedissonRedisChatMemoryRepository customRedissonRedisChatMemoryRepository) {
            sessionIdMessageListMap = customRedissonRedisChatMemoryRepository.getConversations(null);
        } else {
            log.error("未知错误");
        }
        if (sessionIdMessageListMap != null) {
            Set<String> SessIdList = new HashSet<>(sessionIdMessageListMap.keySet());
            SessionIdUtil.BatchValidationResult batchValidationResult = SessionIdUtil.validateBatch(SessIdList);
            Set<String> expiredIds = batchValidationResult.getExpiredIds();
            if (!expiredIds.isEmpty()) {
                //TODO:异步通知
                Result<String> stringResult = sessionArchiveService.sendBatchArchiveMessagesBySessionIdList(expiredIds, userId, agentId);
                log.info(stringResult.getMessage());
            }
            return chatSessionService.getChatSessionVoListBySessionIdMessageListMap(sessionIdMessageListMap, expiredIds);
        }else {
            //降级处理
            List<String> sessionIdList = mySqlChatMemoryRepository.findConversationIds(userId, agentId);
            if (sessionIdList != null) {
                return chatSessionService.getChatSessionVoListBySessionIds(sessionIdList);
            }
        }
        return List.of();
    }

    @Override
    public List<ChatSessionVo> getSessionsPage(ChatSessionContext chatSessionContext) {
        Long agentId = chatSessionContext.getAgentId();
        Long userId = chatSessionContext.getUserId();
        Integer pageNo = chatSessionContext.getPageNo();
        Integer pageSize = chatSessionContext.getPageSize();
        ChatSessionService chatSessionService = chatSessionContext.getChatSessionService();

        ChatMemoryRepository redisChatMemoryRepository = serviceUtil.getRedissonRedisChatMemoryRepository(userId, agentId);
        Map<String, List<Message>> sessionIdMessageListMap = null;
        if (redisChatMemoryRepository instanceof CustomRedissonRedisChatMemoryRepository customRedissonRedisChatMemoryRepository) {
            String redisKeyPrefix = SessionIdUtil.getRedisKeyPrefixByAgentId(agentId);
            List<String> pageConversationIds = customRedissonRedisChatMemoryRepository.findPageConversationIds(userId, redisKeyPrefix, pageNo, pageSize, stringRedisTemplate);
            sessionIdMessageListMap = customRedissonRedisChatMemoryRepository.getConversations(pageConversationIds);
        } else {
            log.error("未知错误");
        }
        if (sessionIdMessageListMap != null) {
            Set<String> SessIdList = new HashSet<>(sessionIdMessageListMap.keySet());
            SessionIdUtil.BatchValidationResult batchValidationResult = SessionIdUtil.validateBatch(SessIdList);
            Set<String> expiredIds = batchValidationResult.getExpiredIds();
            if (!expiredIds.isEmpty()) {
                //TODO:异步通知
                Result<String> stringResult = sessionArchiveService.sendBatchArchiveMessagesBySessionIdList(expiredIds, userId, agentId);
                log.info(stringResult.getMessage());
            }
            return chatSessionService.getChatSessionVoListBySessionIdMessageListMap(sessionIdMessageListMap);
        } else {
            //降级处理
            List<String> sessionIdList = mySqlChatMemoryRepository.findConversationIdsPage(userId, pageNo, pageSize, agentId);
            if (sessionIdList != null) {
                return chatSessionService.getChatSessionVoListBySessionIds(sessionIdList);
            }
        }
        return List.of();
    }

    @Override
    public ChatSessionVo getSessionBySessionId(ChatSessionContext chatSessionContext) {
        Long agentId = chatSessionContext.getAgentId();
        Long userId = chatSessionContext.getUserId();
        String sessionId = chatSessionContext.getSessionId();
        ChatSessionService chatSessionService = chatSessionContext.getChatSessionService();

        SessionIdUtil.ValidationResult validationResult = SessionIdUtil.validateSession(sessionId);
        if (!validationResult.isValid() && !validationResult.isExpired()) {
            throw new AgentException(ResultCodeEnum.SESSION_ID_NO_EXITS);
        }else if (validationResult.isExpired()) {
            //TODO:异步通知
            Result<String> stringResult = sessionArchiveService.sendBatchArchiveMessagesBySessionIdList(List.of(sessionId), userId, agentId);
            log.info(stringResult.getMessage());
            throw new AgentException(ResultCodeEnum.SESSION_ID_EXPIRED);
        }
        ChatMemoryRepository redisChatMemoryRepository = serviceUtil.getRedissonRedisChatMemoryRepository(userId, agentId);
        List<Message> messageList = null;
        if (redisChatMemoryRepository instanceof CustomRedissonRedisChatMemoryRepository customRedissonRedisChatMemoryRepository) {
            messageList = customRedissonRedisChatMemoryRepository.findByConversationId(sessionId);
        } else {
            log.error("未知错误");
        }
        if (messageList != null) {
            List<ChatMessageVo> chatMessageVoList = messageList.stream().map(serviceUtil::getChatMessageVoByMessage).collect(Collectors.toList());
            ChatSessionVo chatSessionVo = serviceUtil.getChatSessionVo(sessionId, chatMessageVoList, chatSessionService);
            if (chatSessionVo != null) {
                return chatSessionVo;
            }
            throw new AgentException(ResultCodeEnum.SESSION_NO_FIND);
        } else {
            //降级处理
            List<ChatSessionVo> chatSessionVoListBySessionIds = chatSessionService.getChatSessionVoListBySessionIds(List.of(sessionId));
            if (chatSessionVoListBySessionIds != null && !chatSessionVoListBySessionIds.isEmpty()) {
                return chatSessionVoListBySessionIds.getFirst();
            }
        }
        return null;
    }


    @Override
    public boolean deleteMemorySessionById(ChatSessionContext chatSessionContext) {
        Long agentId = chatSessionContext.getAgentId();
        Long userId = chatSessionContext.getUserId();
        String sessionId = chatSessionContext.getSessionId();

        try {
            ChatMemoryRepository redisChatMemoryRepository = serviceUtil.getRedissonRedisChatMemoryRepository(userId, agentId);
            redisChatMemoryRepository.deleteByConversationId(sessionId);
        } catch (Exception e) {
            log.error(e.getMessage());
            return false;
        }
        return true;
    }

    @Override
    public long getSessionCount(ChatSessionContext chatSessionContext) {
        Long userId = chatSessionContext.getUserId();
        Long agentId = chatSessionContext.getAgentId();

        String redisKeyPrefix = SessionIdUtil.getRedisKeyPrefixByAgentId(agentId);
        String redisKey = redisKeyPrefix+ userId;
        Set<String> sessionList = stringRedisTemplate.opsForZSet().range(redisKey, 0, -1);
        return sessionList == null ? 0 : sessionList.size();
    }

}
