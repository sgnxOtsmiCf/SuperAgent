package cn.sgnxotsmicf.app.superagent.hook.message;

import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import com.alibaba.cloud.ai.graph.agent.hook.messages.MessagesModelHook;
import com.alibaba.cloud.ai.graph.agent.hook.messages.AgentCommand;
import com.alibaba.cloud.ai.graph.agent.hook.messages.UpdatePolicy;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import org.springframework.ai.chat.messages.Message;
import java.util.List;

@HookPositions({HookPosition.AFTER_MODEL})
public class MessageDeletionHook extends MessagesModelHook {

    @Override
    public String getName() {
        return "message_deletion";
    }

    @Override
    public AgentCommand afterModel(List<Message> previousMessages, RunnableConfig config) {
        if (previousMessages.size() > 2) {
        // 删除最早的两条消息，只保留剩余的消息
        List<Message> remainingMessages = previousMessages.subList(2, previousMessages.size());
        return new AgentCommand(remainingMessages, UpdatePolicy.REPLACE);
    }

    return new AgentCommand(previousMessages);
    }
}