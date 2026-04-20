package cn.sgnxotsmicf.app.superagent.interceptor;

import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallResponse;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ToolCacheInterceptor extends ToolInterceptor {

    // 缓存包装类，存储响应和时间戳
    private static class CacheEntry {
        final ToolCallResponse response;
        final long timestamp;

        CacheEntry(ToolCallResponse response) {
            this.response = response;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired(long ttlMs) {
            return System.currentTimeMillis() - timestamp > ttlMs;
        }
    }

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final long ttlMs;

    public ToolCacheInterceptor() {
        // 默认 5 分钟
        this(5 * 60 * 1000);
    }

    public ToolCacheInterceptor(long ttlMs) {
        this.ttlMs = ttlMs;
    }

    @Override
    public ToolCallResponse interceptToolCall(ToolCallRequest request, ToolCallHandler handler) {
        String cacheKey = generateCacheKey(request);

        // 检查缓存
        CacheEntry entry = cache.get(cacheKey);
        if (entry != null && !entry.isExpired(ttlMs)) {
            log.info("缓存命中: tool={}, key={}", request.getToolName(), cacheKey);
            return entry.response;
        }

        // 缓存未命中或已过期，移除旧缓存
        if (entry != null) {
            cache.remove(cacheKey);
            log.debug("缓存过期已移除: {}", cacheKey);
        }

        // 执行工具调用
        long startTime = System.currentTimeMillis();
        ToolCallResponse response;
        try {
            response = handler.call(request);
        } catch (Exception e) {
            log.error("工具调用失败: tool={}, error={}", request.getToolName(), e.getMessage());
            throw e;
        }
        long costTime = System.currentTimeMillis() - startTime;

        // 只缓存成功的响应（可根据需要调整）
        if (isCacheable(response)) {
            cache.put(cacheKey, new CacheEntry(response));
            log.info("缓存已写入: tool={}, key={}, cost={}ms",
                    request.getToolName(), cacheKey, costTime);
        }

        return response;
    }

    @Override
    public String getName() {
        return "ToolCacheInterceptor";
    }

    /**
     * 生成缓存键
     */
    private String generateCacheKey(ToolCallRequest request) {
        // 从上下文中获取用户ID
        String userId = "anonymous";
        if (request.getContext() != null) {
            Object userIdObj = request.getContext().get("userId");
            if (userIdObj != null) {
                userId = userIdObj.toString();
            }
        }

        // 使用用户ID、工具名和参数生成唯一键
        String argsKey = request.getArguments() != null
                ? request.getArguments().toString()
                : "null";
        return userId + ":" + request.getToolName() + ":" + argsKey.hashCode();
    }

    /**
     * 判断响应是否可缓存
     */
    private boolean isCacheable(ToolCallResponse response) {
        // 可根据业务需求定制：比如不缓存错误响应
        return response != null;
    }

    /**
     * 定期清理过期缓存（每 5 分钟执行一次）
     */
    @Scheduled(fixedRate = 5 * 60 * 1000)
    public void evictExpiredEntries() {
        int beforeSize = cache.size();
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired(ttlMs));
        int removedCount = beforeSize - cache.size();
        if (removedCount > 0) {
            log.info("清理过期缓存: 移除 {} 条，剩余 {} 条", removedCount, cache.size());
        }
    }

    /**
     * 手动清理特定工具的缓存
     */
    public void invalidate(String toolName) {
        cache.entrySet().removeIf(entry -> entry.getKey().startsWith(toolName + ":"));
        log.info("已清理工具 [{}] 的缓存", toolName);
    }

    /**
     * 清空所有缓存
     */
    public void clear() {
        int size = cache.size();
        cache.clear();
        log.info("已清空所有缓存: {} 条", size);
    }

    /**
     * 获取缓存统计信息
     */
    public CacheStats getStats() {
        return new CacheStats(cache.size(), ttlMs);
    }

    public record CacheStats(int size, long ttlMs) {
    }
}