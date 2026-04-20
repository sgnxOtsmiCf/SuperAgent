package cn.sgnxotsmicf.app.superagent.interceptor.router;

import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class TaskProfileAnalyzer {
    // 关键词定义提取为静态常量，后续可演进为配置中心下发
    private static final List<String> REAL_TIME_KEYWORDS = List.of(
            "实时", "立即", "马上", "quick", "fast", "now", "urgent", "紧急",
            "客服", "support", "聊天", "chat", "对话", "conversation"
    );
    private static final List<String> REASONING_KEYWORDS = List.of(
            "推理", "证明", "推导", "逻辑", "分析", "analyze", "reasoning",
            "为什么", "why", "解释原因", "explain why", "数学", "math",
            "算法", "algorithm", "优化", "optimize", "证明", "prove"
    );
    private static final List<String> CODE_KEYWORDS = List.of(
            "代码", "code", "编程", "programming", "debug", "bug", "算法",
            "function", "class", "api", "refactor", "重构", "review"
    );
    private static final int LONG_CONTEXT_THRESHOLD = 100000;

    public TaskProfile analyze(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return new TaskProfile(0, false, 0, false, false, 0, false, false);
        }
        String combinedText = messages.stream()
                .map(Message::getText)
                .filter(Objects::nonNull)
                .reduce("", String::concat);
        String lastMessage = messages.getLast().getText();
        String lowerText = combinedText.toLowerCase();
        return new TaskProfile(
                calculateComplexityScore(messages, combinedText),
                calculateLatencyScore(lastMessage) > 60,
                calculateChineseRatio(combinedText),
                containsKeywords(lowerText, CODE_KEYWORDS),
                containsKeywords(lowerText, REASONING_KEYWORDS),
                combinedText.length(),
                combinedText.length() > LONG_CONTEXT_THRESHOLD,
                detectToolUseIntent(lowerText)
        );
    }

    private int calculateComplexityScore(List<Message> messages, String combinedText) {
        int score = 0;
        int msgCount = messages.size();
        if (msgCount > 10) score += 30;
        else if (msgCount > 5) score += 15;
        int length = combinedText.length();
        if (length > 5000) score += 25;
        else if (length > 1000) score += 10;
        String lower = combinedText.toLowerCase();
        if (lower.contains("步骤") || lower.contains("step by step") || lower.contains("首先") || lower.contains("first"))
            score += 20;
        if (lower.contains("比较") || lower.contains("对比") || lower.contains("分析") || lower.contains("evaluate"))
            score += 15;
        return Math.min(score, 100);
    }

    private int calculateLatencyScore(String text) {
        int score = 0;
        String lower = text.toLowerCase();
        for (String keyword : REAL_TIME_KEYWORDS) {
            if (lower.contains(keyword.toLowerCase())) score += 20;
        }
        if (text.length() < 50) score += 10;
        return Math.min(score, 100);
    }

    /**
     * 性能优化：摒弃正则表达式，采用内存遍历计算中文字符比例，避免长文本GC压力
     */
    private double calculateChineseRatio(String text) {
        if (text == null || text.isEmpty()) return 0;
        int chineseCount = 0;
        for (int i = 0; i < text.length(); ) {
            int codePoint = text.codePointAt(i);
            if (codePoint >= 0x4E00 && codePoint <= 0x9FA5) {
                chineseCount++;
            }
            i += Character.charCount(codePoint);
        }
        return (double) chineseCount / text.length();
    }

    private boolean detectToolUseIntent(String text) {
        return text.contains("工具") || text.contains("tool") || text.contains("调用") ||
                text.contains("api") || text.contains("查询") || text.contains("search") ||
                text.contains("数据库") || text.contains("db");
    }

    private boolean containsKeywords(String text, List<String> keywords) {
        return keywords.stream().anyMatch(k -> text.contains(k.toLowerCase()));
    }
}