package cn.sgnxotsmicf.service;

import cn.sgnxotsmicf.common.vo.ChatRequest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/3 17:00
 * @Version: 1.0
 * @Description:
 */

public interface SuperAgentService {

    SseEmitter doChatStream(ChatRequest request);
}
