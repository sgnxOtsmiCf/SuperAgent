package cn.sgnxotsmicf.app.superagent.interceptor.router;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DefaultModelRoutingStrategy implements ModelRoutingStrategy {
    private static final double WEIGHT_COMPLEXITY = 0.35;
    private static final double WEIGHT_LATENCY_SENSITIVITY = 0.25;
    private static final double WEIGHT_CHINESE_CONTENT = 0.20;
    private static final double WEIGHT_CODE_TASK = 0.20;
    private static final int COMPLEXITY_THRESHOLD_ECONOMY = 25;
    private static final int COMPLEXITY_THRESHOLD_FAST = 50;
    private static final int COMPLEXITY_THRESHOLD_REASONING = 75;
    private static final double CHINESE_CONTENT_RATIO_THRESHOLD = 0.3;

    @Override
    public DynamicModelRouter.ModelTier decide(TaskProfile profile) {
        double economyScore = 0, fastScore = 0, reasoningScore = 0, ultimateScore = 0;
        // 基于复杂度的基础分配
        if (profile.complexityScore() < COMPLEXITY_THRESHOLD_ECONOMY) {
            economyScore += WEIGHT_COMPLEXITY * 100;
        } else if (profile.complexityScore() < COMPLEXITY_THRESHOLD_FAST) {
            fastScore += WEIGHT_COMPLEXITY * 80;
            economyScore += WEIGHT_COMPLEXITY * 20;
        } else if (profile.complexityScore() < COMPLEXITY_THRESHOLD_REASONING) {
            reasoningScore += WEIGHT_COMPLEXITY * 70;
            fastScore += WEIGHT_COMPLEXITY * 30;
        } else {
            ultimateScore += WEIGHT_COMPLEXITY * 60;
            reasoningScore += WEIGHT_COMPLEXITY * 40;
        }
        // 实时性权重
        if (profile.latencySensitive()) {
            fastScore += WEIGHT_LATENCY_SENSITIVITY * 100;
            if (profile.isLongContext()) fastScore += 20;
        }
        // 中文内容权重
        if (profile.chineseContentRatio() > CHINESE_CONTENT_RATIO_THRESHOLD && profile.complexityScore() < 50) {
            economyScore += WEIGHT_CHINESE_CONTENT * 100;
        }
        // 代码/推理任务权重
        if (profile.isCodingTask() || profile.isReasoningTask()) {
            reasoningScore += WEIGHT_CODE_TASK * 100;
            if (profile.complexityScore() > 85) ultimateScore += WEIGHT_CODE_TASK * 30;
        }
        // 工具调用需求
        if (profile.requiresToolUse()) {
            fastScore += 25;
        }
        // 选择最高分的模型
        Map<DynamicModelRouter.ModelTier, Double> scores = Map.of(
                DynamicModelRouter.ModelTier.ECONOMY, economyScore,
                DynamicModelRouter.ModelTier.FAST, fastScore,
                DynamicModelRouter.ModelTier.REASONING, reasoningScore,
                DynamicModelRouter.ModelTier.ULTIMATE, ultimateScore
        );
        return scores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(DynamicModelRouter.ModelTier.ULTIMATE);
    }
}