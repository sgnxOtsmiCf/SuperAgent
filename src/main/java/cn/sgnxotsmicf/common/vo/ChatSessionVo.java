package cn.sgnxotsmicf.common.vo;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/7 16:50
 * @Version: 1.0
 * @Description:
 */
@Data
public class ChatSessionVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Spring AI 对话会话ID
     */
    private String sessionId;


    /**
     * agentId
     */
    Long agentId;

    /**
     * 会话名称
     */
    private String sessionName;


    /**
     * 最后活跃时间
     */
    private LocalDateTime lastActive;

    /**
     * 消息内容
     */
    private List<ChatMessageVo> content;

    /**
     * 是否置顶
     */
    private String isTop;

}
