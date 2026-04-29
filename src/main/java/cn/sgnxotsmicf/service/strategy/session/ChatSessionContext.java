package cn.sgnxotsmicf.service.strategy.session;

import cn.sgnxotsmicf.service.ChatSessionService;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatSessionContext {
    private Long userId;
    private Long agentId;
    private String sessionId;
    private Integer pageNo;
    private Integer pageSize;
    
    // 依赖服务
    private ChatSessionService chatSessionService;
}
