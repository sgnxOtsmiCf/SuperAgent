package cn.sgnxotsmicf.app.manus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Agent消息封装类，用于区分不同类型的消息
 * 与StreamingHandler的SSE事件类型保持一致
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentMessage {

    /**
     * 消息类型，对应SSE event name
     * - thinking: 思考过程
     * - message: 普通消息
     * - toolUsage: 工具调用请求
     * - toolResponse: 工具响应结果
     * - error: 错误信息
     */
    private String type;

    /**
     * 消息内容
     */
    private Object data;

    /**
     * 创建思考消息
     */
    public static AgentMessage thinking(Object content) {
        return new AgentMessage("thinking", content);
    }

    /**
     * 创建普通消息
     */
    public static AgentMessage message(Object content) {
        return new AgentMessage("message", content);
    }

    /**
     * 创建工具调用消息
     */
    public static AgentMessage toolUsage(Object content) {
        return new AgentMessage("toolUsage", content);
    }

    /**
     * 创建工具响应消息
     */
    public static AgentMessage toolResponse(Object content) {
        return new AgentMessage("toolResponse", content);
    }

    /**
     * 创建错误消息
     */
    public static AgentMessage error(Object content) {
        return new AgentMessage("error", content);
    }
}
