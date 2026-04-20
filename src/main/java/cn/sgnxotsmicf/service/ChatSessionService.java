package cn.sgnxotsmicf.service;

import cn.sgnxotsmicf.common.po.ChatSession;
import cn.sgnxotsmicf.common.result.Result;
import cn.sgnxotsmicf.common.vo.ChatSessionVo;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.security.auth.message.AuthException;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/6 11:14
 * @Version: 1.0
 * @Description:
 */

@Service
public interface ChatSessionService extends IService<ChatSession> {

    Result<List<ChatSessionVo>> getSessions(Long agentId);

    Result<List<ChatSessionVo>> getSessionsPage(Long agentId, Integer pageNo, Integer pageSize);

    Result<ChatSessionVo> getSessionBySessionId(Long agentId, String sessionId);

    Result<String> deleteSessionById(Long agentId, String sessionId);

    Result<String> setNameBySessionId(Long agentId, String sessionId, String sessionName);

    Result<String> setTopSession(Long agentId, String sessionId);

    Result<String> setUpTopSession(Long agentId, String sessionId);

    List<ChatSessionVo> getChatSessionVoListBySessionIdMessageListMap(Map<String, List<Message>> sessionIdMessageListMap, Set<String> expiredIds);

    List<ChatSessionVo> getChatSessionVoListBySessionIdMessageListMap(Map<String, List<Message>> sessionIdMessageListMap);

    List<ChatSessionVo> getChatSessionVoListBySessionIds(List<String> sessionIdList);
}
