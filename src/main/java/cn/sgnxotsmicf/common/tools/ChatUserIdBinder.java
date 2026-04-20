package cn.sgnxotsmicf.common.tools;

import org.springframework.stereotype.Component;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spring AI 对话ID -> 用户ID 绑定器
 */
@Component
public class ChatUserIdBinder {
    // 线程安全的缓存，核心：conversationId -> userId
    private static final ConcurrentHashMap<String, Long> CACHE = new ConcurrentHashMap<>();

    // 绑定：对话ID ↔ 用户ID
    public static void bind(String conversationId, Long userId) {
        CACHE.put(conversationId, userId);
    }

    // 获取：根据对话ID拿用户ID
    public static Long getUserId(String conversationId) {
        return CACHE.get(conversationId);
    }

    // 解绑：用完立即清理（必须！）
    public static void unbind(String conversationId) {
        CACHE.remove(conversationId);
    }
}