package cn.sgnxotsmicf.app.superagent.hook.agent;

import com.alibaba.cloud.ai.graph.agent.hook.AgentHook;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;


/**
 * @Author: lixiang
 * @CreateDate: 2026/5/5 01:09
 * @Version: 1.0
 * @Description:
 */
@Slf4j
@HookPositions({HookPosition.AFTER_AGENT})
@Component
public class TotalTokenSummaryHook extends AgentHook {

    private static final String TOTAL_PROMPT_KEY = "_total_prompt_tokens_";
    private static final String TOTAL_COMPLETION_KEY = "_total_completion_tokens_";
    private static final String TOTAL_KEY = "_total_tokens_";

    @Override
    public String getName() {
        return "totalTokenSummary";
    }

    @Override
    public HookPosition[] getHookPositions() {
        return new HookPosition[]{HookPosition.AFTER_AGENT};
    }

    @Override
    public CompletableFuture<Map<String, Object>> afterAgent(OverAllState state, RunnableConfig config) {
        Object totalTokens = config.context().get(TOTAL_KEY);
        Object promptTokens = config.context().get(TOTAL_COMPLETION_KEY);
        Object completionTokens = config.context().get(TOTAL_PROMPT_KEY);
        if (totalTokens != null && promptTokens != null && completionTokens != null) {
            int currentRound = (int) config.context().getOrDefault("__model_call_limit_run_count__", 0);
            log.info(
                    "【ReAct执行完毕 共{} 轮】Prompt Tokens: {} | Completion Tokens: {} | Total Tokens: {}",
                    currentRound,
                    promptTokens,
                    completionTokens,
                    totalTokens
            );
        }
        return CompletableFuture.completedFuture(Map.of());
    }
}