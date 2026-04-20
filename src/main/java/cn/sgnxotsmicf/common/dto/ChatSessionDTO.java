package cn.sgnxotsmicf.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/18 12:24
 * @Version: 1.0
 * @Description: AI对话会话 创建DTO 用于RabbitMQ消息传输、前端提交创建会话参数
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSessionDTO implements Serializable {


    @Serial
    private static final long serialVersionUID = 1L;

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
    private Long agentId;

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
     * 重试次数
     */
    private Integer retryCount;

    /**
     * 操作类型：插入|更新
     */
    private String doType;

    /**
     * 触发方式常量
     */
    public static final String INSERT = "insert";
    public static final String UPDATE = "update";


    /**
     * 增加重试次数
     */
    public void incrementRetry() {
        this.retryCount = (this.retryCount == null ? 0 : this.retryCount) + 1;
    }
}