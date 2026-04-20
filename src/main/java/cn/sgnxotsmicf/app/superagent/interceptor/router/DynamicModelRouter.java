package cn.sgnxotsmicf.app.superagent.interceptor.router;

import cn.sgnxotsmicf.app.superagent.interceptor.router.ModelRoutingStrategy;
import cn.sgnxotsmicf.app.superagent.interceptor.router.TaskProfile;
import cn.sgnxotsmicf.app.superagent.interceptor.router.TaskProfileAnalyzer;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.zhipuai.ZhiPuAiChatModel;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
public class DynamicModelRouter{
    public enum ModelTier {
        ECONOMY("经济型", "GLM-4.5-Air", 0.6),
        FAST("极速型", "Grok-4.1-fast", 2.0),
        REASONING("推理型", "DeepSeek-R1", 1.5),
        ULTIMATE("终极型", "Qwen-Max", 3.0);
        public final String displayName;
        public final String modelName;
        public final double costPerMToken;

        ModelTier(String displayName, String modelName, double cost) {
            this.displayName = displayName;
            this.modelName = modelName;
            this.costPerMToken = cost;
        }
    }

    private final TaskProfileAnalyzer profileAnalyzer;
    private final ModelRoutingStrategy routingStrategy;
    private final Map<ModelTier, ChatModel> modelRegistry;
    // 监控与治理
    private final Map<ModelTier, RoutingMetrics> tierMetrics = new ConcurrentHashMap<>();
    private final Map<String, ModelTier> sessionOverride = new ConcurrentHashMap<>();
    private final Map<String, List<RoutingDecision>> decisionLog = new ConcurrentHashMap<>();

    public DynamicModelRouter(
            TaskProfileAnalyzer profileAnalyzer,
            ModelRoutingStrategy routingStrategy,
            ZhiPuAiChatModel zhiPuAiChatModel,
            OpenAiChatModel xAiGrokChatModel,
            DeepSeekChatModel deepSeekChatModel,
            DashScopeChatModel dashScopeChatModel
    ) {
        this.profileAnalyzer = profileAnalyzer;
        this.routingStrategy = routingStrategy;
        // 初始化模型注册表，解除硬编码的 if-else/switch 分发
        this.modelRegistry = Map.of(
                ModelTier.ECONOMY, zhiPuAiChatModel,
                ModelTier.FAST, xAiGrokChatModel,
                ModelTier.REASONING, deepSeekChatModel,
                ModelTier.ULTIMATE, dashScopeChatModel
        );
        for (ModelTier tier : ModelTier.values()) {
            tierMetrics.put(tier, new RoutingMetrics());
        }
        log.info("四模型动态路由系统初始化完成");
    }
    
    public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
        long startTime = System.currentTimeMillis();
        String sessionId = extractSessionId(request);
        try {
            // 1. 检查手动覆盖
            if (sessionOverride.containsKey(sessionId)) {
                ModelTier overrideTier = sessionOverride.get(sessionId);
                log.debug("会话 {} 使用手动指定模型: {}", sessionId, overrideTier);
                return executeWithTier(request, handler, overrideTier, startTime, sessionId);
            }
            // 2. 任务画像分析 (委托给 Analyzer)
            TaskProfile profile = profileAnalyzer.analyze(request.getMessages());
            // 3. 智能路由决策 (委托给 Strategy)
            ModelTier selectedTier = routingStrategy.decide(profile);
            // 4. 记录决策日志
            logRoutingDecision(sessionId, selectedTier, profile);
            // 5. 执行调用
            ModelResponse response = executeWithTier(request, handler, selectedTier, startTime, sessionId);
            // 6. 更新成功指标
            updateSuccessMetrics(selectedTier, System.currentTimeMillis() - startTime);
            return response;
        } catch (Exception e) {
            log.error("模型路由失败，执行兜底策略: {}", e.getMessage());
            return executeFallback(request, handler, startTime, sessionId);
        }
    }

    private String extractSessionId(ModelRequest request) {
        Map<String, Object> context = request.getContext();
        if (context != null) {
            Object sessionId = context.getOrDefault("sessionId", "default_value");
            if (sessionId != null) return sessionId.toString();
        }
        return UUID.randomUUID().toString();
    }

    private ModelResponse executeWithTier(ModelRequest request, ModelCallHandler handler,
                                          ModelTier tier, long startTime, String sessionId) {
        ChatModel model = modelRegistry.get(tier);
        if (model == null) {
            throw new IllegalStateException("未找到模型等级对应的 ChatModel 实例: " + tier);
        }
        log.info("路由决策: {} -> {} (成本: ${}/M tokens) [session={}]",
                tier.displayName, tier.modelName, tier.costPerMToken, sessionId);
        ModelRequest enrichedRequest = ModelRequest.builder(request)
                .context(enrichContext(request.getContext(), tier, sessionId))
                .build();
        return handler.call(enrichedRequest);
    }

    private ModelResponse executeFallback(ModelRequest request, ModelCallHandler handler,
                                          long startTime, String sessionId) {
        log.warn("[session={}] 执行兜底策略，切换到Ultimate模型(Qwen-Max)", sessionId);
        updateFallbackMetrics(ModelTier.ULTIMATE);
        return executeWithTier(request, handler, ModelTier.ULTIMATE, startTime, sessionId);
    }

    private Map<String, Object> enrichContext(Map<String, Object> originalContext, ModelTier tier, String sessionId) {
        Map<String, Object> enriched = new HashMap<>(originalContext != null ? originalContext : Map.of());
        enriched.put("selectedModelTier", tier.name());
        enriched.put("selectedModelName", tier.modelName);
        enriched.put("sessionId", sessionId);
        enriched.put("routingTimestamp", System.currentTimeMillis());
        return enriched;
    }

    private void logRoutingDecision(String sessionId, ModelTier tier, TaskProfile profile) {
        RoutingDecision decision = new RoutingDecision(
                System.currentTimeMillis(), tier, profile.complexityScore(),
                profile.latencySensitive(), profile.chineseContentRatio(),
                profile.isCodingTask(), profile.isReasoningTask()
        );
        // 修复并发安全：使用 CopyOnWriteArrayList
        decisionLog.computeIfAbsent(sessionId, k -> new CopyOnWriteArrayList<>()).add(decision);
        tierMetrics.get(tier).requestCount.incrementAndGet();
    }

    private void updateSuccessMetrics(ModelTier tier, long latencyMs) {
        RoutingMetrics metrics = tierMetrics.get(tier);
        metrics.totalLatency.addAndGet(latencyMs);
        metrics.successCount.incrementAndGet();
    }

    private void updateFallbackMetrics(ModelTier tier) {
        tierMetrics.get(tier).fallbackCount.incrementAndGet();
    }

    // ==================== 管理接口 ====================
    public Map<String, Object> getRoutingStatistics() {
        Map<String, Object> stats = new HashMap<>();
        for (ModelTier tier : ModelTier.values()) {
            RoutingMetrics m = tierMetrics.get(tier);
            Map<String, Object> tierStat = new HashMap<>();
            tierStat.put("requests", m.requestCount.get());
            tierStat.put("success", m.successCount.get());
            tierStat.put("fallbacks", m.fallbackCount.get());
            tierStat.put("avgLatencyMs", m.successCount.get() > 0 ? m.totalLatency.get() / m.successCount.get() : 0);
            tierStat.put("estimatedCostUsd", m.requestCount.get() * tier.costPerMToken / 1_000_000);
            stats.put(tier.name(), tierStat);
        }
        return stats;
    }

    public void setSessionModelOverride(String sessionId, ModelTier tier) {
        sessionOverride.put(sessionId, tier);
        log.info("管理员覆盖会话 {} 的模型选择为 {}", sessionId, tier);
    }

    public void clearSessionOverride(String sessionId) {
        sessionOverride.remove(sessionId);
    }
    
    public String getName() {
        return "EnterpriseDynamicModelRouter-v4";
    }

    public int getOrder() {
        return 100;
    }

    // ==================== 内部类 ====================
    private static class RoutingMetrics {
        final AtomicLong requestCount = new AtomicLong(0);
        final AtomicLong successCount = new AtomicLong(0);
        final AtomicLong fallbackCount = new AtomicLong(0);
        final AtomicLong totalLatency = new AtomicLong(0);
    }

    private record RoutingDecision(
            long timestamp, ModelTier selectedTier, int complexityScore,
            boolean latencySensitive, double chineseRatio,
            boolean isCodingTask, boolean isReasoningTask
    ) {
    }
}