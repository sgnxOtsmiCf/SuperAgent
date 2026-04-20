package cn.sgnxotsmicf.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 会话归档消息实体
 * @Author: lixiang
 * @CreateDate: 2026/4/15 12:24
 * @Version: 1.0
 * @Description: 用于RabbitMQ传输的归档请求消息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArchiveMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 消息唯一ID
     */
    private String messageId;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * Agent ID
     */
    private Long agentId;

    /**
     * 触发方式：AUTO-自动过期触发 MANUAL-手动触发
     */
    private String triggerType;

    /**
     * 消息创建时间
     */
    private LocalDateTime createTime;

    /**
     * 重试次数
     */
    private Integer retryCount;

    /**
     * 触发方式常量
     */
    public static final String TRIGGER_TYPE_AUTO = "AUTO";
    public static final String TRIGGER_TYPE_MANUAL = "MANUAL";

    /**
     * 创建自动归档消息
     */
    public static ArchiveMessage createAutoArchiveMessage(String sessionId, Long userId, Long agentId) {
        return ArchiveMessage.builder()
                .messageId(generateMessageId())
                .sessionId(sessionId)
                .userId(userId)
                .agentId(agentId)
                .triggerType(TRIGGER_TYPE_AUTO)
                .createTime(LocalDateTime.now())
                .retryCount(0)
                .build();
    }

    /**
     * 创建手动归档消息
     */
    public static ArchiveMessage createManualArchiveMessage(String sessionId, Long userId, Long agentId) {
        return ArchiveMessage.builder()
                .messageId(generateMessageId())
                .sessionId(sessionId)
                .userId(userId)
                .agentId(agentId)
                .triggerType(TRIGGER_TYPE_MANUAL)
                .createTime(LocalDateTime.now())
                .retryCount(0)
                .build();
    }

    /**
     * 生成消息ID
     */
    private static String generateMessageId() {
        return "ARCH" + System.currentTimeMillis() + "_" + (int) (Math.random() * 10000);
    }

    /**
     * 增加重试次数
     */
    public void incrementRetry() {
        this.retryCount = (this.retryCount == null ? 0 : this.retryCount) + 1;
    }
}
