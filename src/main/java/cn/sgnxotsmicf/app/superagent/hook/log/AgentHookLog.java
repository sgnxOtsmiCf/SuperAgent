package cn.sgnxotsmicf.app.superagent.hook.log;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.hook.AgentHook;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/3 14:59
 * @Version: 1.0
 * @Description:
 */
@HookPositions({HookPosition.BEFORE_AGENT, HookPosition.AFTER_AGENT})
@Component
@Slf4j
public class AgentHookLog extends AgentHook {

    @Override
    public String getName() {
        return "AgentHookLog";
    }

    @Override
    public CompletableFuture<Map<String, Object>> beforeAgent(OverAllState state, RunnableConfig config) {
        log.info("【SuperAgent】开始执行...");
        return super.beforeAgent(state, config);
    }

    @Override
    public CompletableFuture<Map<String, Object>> afterAgent(OverAllState state, RunnableConfig config) {
        log.info("【SuperAgent】结束执行...");
        return super.afterAgent(state, config);
    }
}
