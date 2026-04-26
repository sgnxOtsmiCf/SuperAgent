package cn.sgnxotsmicf.common.rabbitmq.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * 会话归档消息实体
 * @Author: lixiang
 * @CreateDate: 2026/4/15 12:24
 * @Version: 1.0
 * @Description: 用于 RabbitMQ 传输的归档请求消息
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ArchiveMessage extends BaseRabbitMessage {

    /**
     * 触发方式：AUTO-自动过期触发 MANUAL-手动触发
     */
    private String triggerType;

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
                .messageId(generateMessageId("ARCH"))
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
                .messageId(generateMessageId("ARCH"))
                .sessionId(sessionId)
                .userId(userId)
                .agentId(agentId)
                .triggerType(TRIGGER_TYPE_MANUAL)
                .createTime(LocalDateTime.now())
                .retryCount(0)
                .build();
    }
}