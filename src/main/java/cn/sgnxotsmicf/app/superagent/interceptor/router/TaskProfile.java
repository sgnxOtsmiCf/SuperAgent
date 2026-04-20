package cn.sgnxotsmicf.app.superagent.interceptor.router;

public record TaskProfile(
        int complexityScore,
        boolean latencySensitive,
        double chineseContentRatio,
        boolean isCodingTask,
        boolean isReasoningTask,
        int contextLength,
        boolean isLongContext,
        boolean requiresToolUse
) { }