package cn.sgnxotsmicf.common.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * AI对话消息实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "superagent_chat_message",autoResultMap = true)
public class ChatMessage extends BaseEntity {


    /**
     * Spring AI 对话会话ID
     */
    private String sessionId;

    /**
     * 消息类型（Spring AI标准枚举）
     * USER/ASSISTANT/SYSTEM/TOOL
     */
    private String messageType;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息发送时间
     */
    private LocalDateTime messageTime;

    /**
     * 元数据（存储Spring AI扩展信息：token、模型、请求ID等）
     * MyBatis-Plus 自动映射JSON字段
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> metadata;
}