package cn.sgnxotsmicf.app.manus.model;

import cn.sgnxotsmicf.app.manus.AgentMessage;
import lombok.Data;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatResponse;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

@Data
public class AgentContext {

    private AgentState state = AgentState.IDLE;

    private int currentStep = 0;

    private List<Message> messageList = new ArrayList<>();

    /**
     * 带类型的消息队列，用于流式传输
     */
    private Queue<AgentMessage> agentMessageQueue = new ArrayDeque<>();

    private String sessionId;

    private ChatResponse toolCallChatResponse;

    public AgentContext(String sessionId) {
        this.sessionId = sessionId;
    }

    public AgentContext() {
    }

    /**
     * 添加思考消息到队列
     */
    public void addThinkingMessage(Object content) {
        this.agentMessageQueue.offer(AgentMessage.thinking(content));
    }

    /**
     * 添加普通消息到队列
     */
    public void addMessage(Object content) {
        this.agentMessageQueue.offer(AgentMessage.message(content));
    }

    /**
     * 添加工具调用消息到队列
     */
    public void addToolUsageMessage(Object content) {
        this.agentMessageQueue.offer(AgentMessage.toolUsage(content));
    }

    /**
     * 添加工具响应消息到队列
     */
    public void addToolResponseMessage(Object content) {
        this.agentMessageQueue.offer(AgentMessage.toolResponse(content));
    }

    /**
     * 添加错误消息到队列
     */
    public void addErrorMessage(Object content) {
        this.agentMessageQueue.offer(AgentMessage.error(content));
    }
}
