package cn.sgnxotsmicf.common.rabbitmq.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * RabbitMQ 消息基础实体
 * @Author: lixiang
 * @CreateDate: 2026/4/26
 * @Version: 1.0
 * @Description: 提取 RabbitMQ 消息的通用属性
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseRabbitMessage implements Serializable {

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
     * 消息创建时间
     */
    private LocalDateTime createTime;

    /**
     * 重试次数
     */
    private Integer retryCount;

    /**
     * 增加重试次数
     */
    public void incrementRetry() {
        this.retryCount = (this.retryCount == null ? 0 : this.retryCount) + 1;
    }

    /**
     * 生成消息ID
     *
     * @param prefix 消息前缀
     */
    protected static String generateMessageId(String prefix) {
        return prefix + System.currentTimeMillis() + "_" + (int) (Math.random() * 10000);
    }
}