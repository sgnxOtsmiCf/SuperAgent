package cn.sgnxotsmicf.app.superagent.interceptor;

import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelCallHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * @Author: lixiang
 * @CreateDate: 2026/5/5 01:07
 * @Version: 1.0
 * @Description:
 */

@Slf4j
@Component
public class ReActTokenUsageInterceptor extends ModelInterceptor {

    private static final String TOTAL_PROMPT_KEY = "_total_prompt_tokens_";
    private static final String TOTAL_COMPLETION_KEY = "_total_completion_tokens_";
    private static final String TOTAL_KEY = "_total_tokens_";

    @Override
    public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
        // 执行实际模型调用
        ModelResponse response = handler.call(request);
        ChatResponse chatResponse = response.getChatResponse();
        if (chatResponse != null && !chatResponse.getMetadata().isEmpty()) {
            int currentRound = (int) request.getContext().getOrDefault("__model_call_limit_run_count__", 0);
            Usage usage = chatResponse.getMetadata().getUsage();
            if (usage != null) {
                // 累加到 RunnableConfig 上下文（跨迭代共享）
                Map<String, Object> ctx = request.getContext();

                long totalPrompt = (long) ctx.getOrDefault(TOTAL_PROMPT_KEY, 0L) + usage.getPromptTokens();
                long totalCompletion = (long) ctx.getOrDefault(TOTAL_COMPLETION_KEY, 0L) + usage.getCompletionTokens();
                long total = (long) ctx.getOrDefault(TOTAL_KEY, 0L) + usage.getTotalTokens();

                ctx.put(TOTAL_PROMPT_KEY, totalPrompt);
                ctx.put(TOTAL_COMPLETION_KEY, totalCompletion);
                ctx.put(TOTAL_KEY, total);
                log.info(
                    "【ReAct 第 {} 轮】Prompt Tokens: {} | Completion Tokens: {} | Total Tokens: {}",
                    currentRound,
                    usage.getPromptTokens(),
                    usage.getCompletionTokens(),
                    usage.getTotalTokens()
                );
            } else {
                log.info("【ReAct 第 {} 轮】Usage 为空（模型未返回 Token 信息）%n", currentRound);
            }
        }
        
        return response;
    }

    @Override
    public String getName() {
        return "ReActTokenUsageInterceptor";
    }
}