package cn.sgnxotsmicf.app.superagent.hook.message;

import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import com.alibaba.cloud.ai.graph.agent.hook.messages.MessagesModelHook;
import com.alibaba.cloud.ai.graph.agent.hook.messages.AgentCommand;
import com.alibaba.cloud.ai.graph.agent.hook.messages.UpdatePolicy;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import java.util.ArrayList;
import java.util.List;

@HookPositions({HookPosition.AFTER_MODEL})
public class ValidateResponseHook extends MessagesModelHook {

      private static final List<String> STOP_WORDS =
          List.of("password", "secret", "api_key");

      @Override
      public String getName() {
          return "validate_response";
      }

      @Override
      public AgentCommand afterModel(List<Message> previousMessages, RunnableConfig config) {
          if (previousMessages.isEmpty()) {
              return new AgentCommand(previousMessages);
          }

          Message lastMessage = previousMessages.getLast();
          String content = lastMessage.getText();

          // 检查是否包含敏感词
          for (String stopWord : STOP_WORDS) {
              if (content.toLowerCase().contains(stopWord)) {
                  // 移除包含敏感词的消息，替换为安全消息
                  List<Message> filtered = new ArrayList<>(
                      previousMessages.subList(0, previousMessages.size() - 1)
                  );
                  filtered.add(new AssistantMessage("抱歉，我无法提供该信息。"));
                  return new AgentCommand(filtered, UpdatePolicy.REPLACE);
              }
          }

          return new AgentCommand(previousMessages);
      }
}