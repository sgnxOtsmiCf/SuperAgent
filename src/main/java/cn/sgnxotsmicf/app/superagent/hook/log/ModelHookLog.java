package cn.sgnxotsmicf.app.superagent.hook.log;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import com.alibaba.cloud.ai.graph.agent.hook.ModelHook;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/3 15:10
 * @Version: 1.0
 * @Description:
 */
@Slf4j
@Component
@HookPositions({HookPosition.BEFORE_MODEL, HookPosition.AFTER_MODEL})
public class ModelHookLog extends ModelHook {

    @Override
    public String getName() {
        return "ModelHookLog";
    }

    @Override
    public CompletableFuture<Map<String, Object>> beforeModel(OverAllState state, RunnableConfig config) {

        return super.beforeModel(state, config);
    }

    @Override
    public CompletableFuture<Map<String, Object>> afterModel(OverAllState state, RunnableConfig config) {
        int currentRound = (int) config.context().getOrDefault("__model_call_limit_run_count__", 0);
        // 1. 获取完整消息列表
        Optional<Object> messagesOpt = state.value("messages");
        if (messagesOpt.isPresent()) {
            List<Message> messages = (List<Message>) messagesOpt.get();
            // 2. 提取最后一条 AssistantMessage（当前轮次模型输出）
            for (int i = messages.size()-1; i >= 0; i--) {
                Message message = messages.get(i);
                if (message instanceof AssistantMessage assistantMessage) {
                    // 3. 处理模型输出（示例：日志打印）
                    System.out.println("=== 第"+currentRound+"轮 ===");
                    System.out.println("【SuperAgent】:" + assistantMessage.getText());
                    // 4. 工具调用信息（如有）
                    if (!assistantMessage.getToolCalls().isEmpty()) {
                        System.out.println("工具调用:");
                        assistantMessage.getToolCalls().forEach(toolCall ->
                                System.out.println("  - " + toolCall.name() + ": " + toolCall.arguments())
                        );
                    }
                    System.out.println("=======================");
                    break; // 找到最后一条模型输出即可退出
                }
            }
        }

        return CompletableFuture.completedFuture(Map.of());
    }
}
