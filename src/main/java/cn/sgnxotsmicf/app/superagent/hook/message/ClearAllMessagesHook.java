package cn.sgnxotsmicf.app.superagent.hook.message;

import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import com.alibaba.cloud.ai.graph.agent.hook.messages.MessagesModelHook;
import com.alibaba.cloud.ai.graph.agent.hook.messages.AgentCommand;
import com.alibaba.cloud.ai.graph.agent.hook.messages.UpdatePolicy;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import org.springframework.ai.chat.messages.Message;
import java.util.ArrayList;
import java.util.List;

@HookPositions({HookPosition.AFTER_MODEL})
public class ClearAllMessagesHook extends MessagesModelHook {

    @Override
    public String getName() {
        return "clear_all_messages";
    }

    @Override
    public AgentCommand afterModel(List<Message> previousMessages, RunnableConfig config) {
        // 删除所有消息，返回空列表
        return new AgentCommand(new ArrayList<>(), UpdatePolicy.REPLACE);
    }
}