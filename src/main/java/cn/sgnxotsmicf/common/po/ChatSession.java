package cn.sgnxotsmicf.common.po;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * AI对话会话实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("superagent_chat_session")
public class ChatSession extends BaseEntity {

    /**
     * 用户ID
     */
    private Long userId;

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
     * 会话状态：1-活跃 0-归档 2-禁用
     */
    private Integer sessionStatus;
}