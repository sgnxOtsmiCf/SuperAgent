package cn.sgnxotsmicf.service.strategy.strategyImpl;

import cn.sgnxotsmicf.app.superagent.factory.SuperAgentFactory;
import cn.sgnxotsmicf.common.result.ResultCodeEnum;
import cn.sgnxotsmicf.common.agent.AgentCommon;
import cn.sgnxotsmicf.common.tools.ServiceUtil;
import cn.sgnxotsmicf.common.vo.ChatSessionVo;
import cn.sgnxotsmicf.exception.AgentException;
import cn.sgnxotsmicf.service.ChatSessionService;
import cn.sgnxotsmicf.service.strategy.ChatSessionContext;
import cn.sgnxotsmicf.service.strategy.ChatSessionStrategy;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.redis.RedisSaver;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/17 12:05
 * @Version: 1.0
 * @Description:
 */

@Slf4j
@Component(AgentCommon.SuperAgent)
public class SuperAgentChatSessionStrategy implements ChatSessionStrategy {

    @Resource
    private SuperAgentFactory superAgentFactory;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private ServiceUtil serviceUtil;

    @Override
    public boolean supports(Long agentId) {
        return AgentCommon.SuperAgentId.equals(agentId);
    }

    @Override
    public List<ChatSessionVo> getSessions(ChatSessionContext chatSessionContext) {
        Long userId = chatSessionContext.getUserId();
        ChatSessionService chatSessionService = chatSessionContext.getChatSessionService();

        RedisSaver redisSaver = superAgentFactory.buildRedisSaver();
        Set<String> sessionList = stringRedisTemplate.opsForZSet().range(AgentCommon.RedisSuperAgentKeyPrefix + userId, 0, -1);
        if (sessionList == null || sessionList.isEmpty()) {
            throw new AgentException(ResultCodeEnum.SESSION_NO_FIND);
        }
        //根据列表通过RedisSaver进行查询
        return serviceUtil.getChatSessionVoList(sessionList, redisSaver, chatSessionService);
    }

    @Override
    public List<ChatSessionVo> getSessionsPage(ChatSessionContext chatSessionContext) {
        Integer pageNo = chatSessionContext.getPageNo();
        Integer pageSize = chatSessionContext.getPageSize();
        ChatSessionService chatSessionService = chatSessionContext.getChatSessionService();
        Long userId = chatSessionContext.getUserId();

        int offset = (pageNo - 1) * pageSize;
        Set<String> sessionList = stringRedisTemplate
                .opsForZSet()
                .reverseRangeByScore(AgentCommon.RedisSuperAgentKeyPrefix + userId, 0, System.currentTimeMillis(), offset, pageSize);
        if (sessionList == null || sessionList.isEmpty()) {
            throw new AgentException(ResultCodeEnum.SESSION_NO_FIND);
        }
        sessionList = sessionList.stream().skip((long) (pageNo - 1) * pageSize).limit(pageSize).collect(Collectors.toSet());
        RedisSaver redisSaver = superAgentFactory.buildRedisSaver();
        return serviceUtil.getChatSessionVoList(sessionList, redisSaver, chatSessionService);
    }

    @Override
    public ChatSessionVo getSessionBySessionId(ChatSessionContext chatSessionContext) {
        String sessionId = chatSessionContext.getSessionId();
        ChatSessionService chatSessionService = chatSessionContext.getChatSessionService();
        RedisSaver redisSaver = superAgentFactory.buildRedisSaver();
        List<ChatSessionVo> chatSessionVoList = serviceUtil.getChatSessionVoList(Set.of(sessionId), redisSaver, chatSessionService);
        if (chatSessionVoList == null || chatSessionVoList.isEmpty()) {
            throw new AgentException(ResultCodeEnum.SESSION_NO_FIND);
        }
        return chatSessionVoList.getFirst();
    }

    @Override
    public boolean deleteMemorySessionById(ChatSessionContext chatSessionContext) {
        String sessionId = chatSessionContext.getSessionId();

        RedisSaver redisSaver = superAgentFactory.buildRedisSaver();
        try {
            redisSaver.release(RunnableConfig.builder().threadId(sessionId).build());
        } catch (Exception e) {
            log.error(e.getMessage());
            return false;
        }
        return true;
    }

    @Override
    public long getSessionCount(ChatSessionContext chatSessionContext) {
        Long userId = chatSessionContext.getUserId();

        String redisKey = AgentCommon.RedisSuperAgentKeyPrefix + userId;
        Set<String> sessionList = stringRedisTemplate.opsForZSet().range(redisKey, 0, -1);
        return sessionList == null ? 0 : sessionList.size();
    }
}
