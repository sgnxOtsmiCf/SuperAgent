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

@HookPositions({HookPosition.BEFORE_MODEL})
public class MessageTrimmingHook extends MessagesModelHook {

  private static final int MAX_MESSAGES = 15;

  @Override
  public String getName() {
      return "message_trimming";
  }

  @Override
  public AgentCommand beforeModel(List<Message> previousMessages, RunnableConfig config) {
      if (previousMessages.size() <= MAX_MESSAGES) {
          return new AgentCommand(previousMessages); // 无需更改
      }

      // 保留第一条消息和最后几条消息---超过10条,裁剪4~5条
      Message firstMsg = previousMessages.getFirst();
      int keepCount = previousMessages.size() % 2 == 0 ? 4 : 6;
      List<Message> recentMessages = previousMessages.subList(
          previousMessages.size() - keepCount,
          previousMessages.size()
      );

      List<Message> trimmedMessages = new ArrayList<>();
      trimmedMessages.add(firstMsg);
      trimmedMessages.addAll(recentMessages);

      // 使用 REPLACE 策略替换消息列表，只保留需要的消息
      return new AgentCommand(trimmedMessages, UpdatePolicy.REPLACE);
  }
}