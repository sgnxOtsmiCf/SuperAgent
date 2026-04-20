package cn.sgnxotsmicf.service;

import cn.sgnxotsmicf.common.vo.ChatRequest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/8 21:05
 * @Version: 1.0
 * @Description:
 */

public interface OpenManusService {

    boolean sessionIdIsExist(String sessionId);

    SseEmitter doChatWithSseEmitter(ChatRequest chatRequest);
}
