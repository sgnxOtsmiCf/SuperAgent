package cn.sgnxotsmicf.common.rabbitmq.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * AI 对话会话同步消息实体
 * @Author: lixiang
 * @CreateDate: 2026/4/18 12:24
 * @Version: 1.0
 * @Description: 用于 RabbitMQ 消息传输的会话同步消息
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SessionMessage extends BaseRabbitMessage {

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

    /**
     * 操作类型：insert-插入 update-更新
     */
    private String doType;

    /**
     * 操作类型常量
     */
    public static final String INSERT = "insert";
    public static final String UPDATE = "update";


    /**
     * 会话状态常量
     */
    public static final Integer STATUS_ACTIVE = 1;
    public static final Integer STATUS_ARCHIVED = 0;
    public static final Integer STATUS_DISABLED = 2;

    /**
     * 创建会话插入消息
     *
     * @param sessionId   会话ID
     * @param userId      用户ID
     * @param agentId     Agent ID
     * @param sessionName 会话名称
     * @return 会话插入消息
     */
    public static SessionMessage createInsertMessage(String sessionId, Long userId, Long agentId, String sessionName) {
        return SessionMessage.builder()
                .messageId(generateMessageId("CHAT"))
                .sessionId(sessionId)
                .userId(userId)
                .agentId(agentId)
                .sessionName(sessionName)
                .lastActive(LocalDateTime.now())
                .sessionStatus(STATUS_ACTIVE)
                .doType(INSERT)
                .createTime(LocalDateTime.now())
                .retryCount(0)
                .build();
    }

    /**
     * 创建会话更新消息
     *
     * @param sessionId   会话ID
     * @param userId      用户ID
     * @param agentId     Agent ID
     * @param sessionName 会话名称
     * @return 会话更新消息
     */
    public static SessionMessage createUpdateMessage(String sessionId, Long userId, Long agentId, String sessionName) {
        return SessionMessage.builder()
                .messageId(generateMessageId("CHAT"))
                .sessionId(sessionId)
                .userId(userId)
                .agentId(agentId)
                .sessionName(sessionName)
                .lastActive(LocalDateTime.now())
                .sessionStatus(STATUS_ACTIVE)
                .doType(UPDATE)
                .createTime(LocalDateTime.now())
                .retryCount(0)
                .build();
    }

    /**
     * 创建会话状态更新消息（仅更新状态和最后活跃时间）
     *
     * @param sessionId    会话ID
     * @param userId       用户ID
     * @param agentId      Agent ID
     * @param sessionName  会话名称
     * @param sessionStatus 目标会话状态
     * @return 会话状态更新消息
     */
    public static SessionMessage createStatusUpdateMessage(String sessionId, Long userId, Long agentId,
                                                               String sessionName, Integer sessionStatus) {
        return SessionMessage.builder()
                .messageId(generateMessageId("CHAT"))
                .sessionId(sessionId)
                .userId(userId)
                .agentId(agentId)
                .sessionName(sessionName)
                .lastActive(LocalDateTime.now())
                .sessionStatus(sessionStatus)
                .doType(UPDATE)
                .createTime(LocalDateTime.now())
                .retryCount(0)
                .build();
    }


}