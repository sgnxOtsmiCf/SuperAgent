package cn.sgnxotsmicf.app.superagent.hook.model;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import com.alibaba.cloud.ai.graph.agent.hook.ModelHook;
import com.alibaba.cloud.ai.graph.store.Store;
import com.alibaba.cloud.ai.graph.store.StoreItem;
import com.alibaba.druid.support.profile.ProfileEntry;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.zhipuai.ZhiPuAiChatModel;
import org.springframework.stereotype.Component;
import jakarta.annotation.Resource;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/19 20:48
 * @Version: 4.0
 * @Description: 动态用户画像记忆Hook，基于LLM进行增量事实提取与合并，支持时间衰减与容量淘汰
 */
@Component
@Slf4j
@HookPositions(value = {HookPosition.BEFORE_MODEL, HookPosition.AFTER_MODEL})
public class MemoryHook extends ModelHook {

    private static final String USER_PROFILE_NAMESPACE = "user_profiles";

    // 画像最大保存条数，防止无限膨胀
    private static final int MAX_PROFILE_SIZE = 50;

    // 画像过期时间：30天未更新则淘汰
    private static final long PROFILE_EXPIRE_DAYS = 30;

    // 最小输入字数限制，少于该值不触发提取
    private static final int MIN_INPUT_LENGTH = 10;

    private static final String EXTRACTION_SYSTEM_PROMPT = """
            你是一个专业的用户画像信息提取助手。
            你的任务是根据提供的【当前用户输入】，提取出关于用户的客观属性或主观偏好，并与已有画像合并。
            必须以纯JSON键值对格式输出，Key为你提取的维度名称(如"偏好颜色"、"姓名"、"技术栈")，Value为对应的具体内容。
            如果当前用户输入中提取不到任何有效的偏好或属性信息，请务必输出空对象 {}。
            禁止输出Markdown标记或解释性文字，仅输出JSON本身。
            """;
    @Resource
    private ZhiPuAiChatModel zhiPuAiChatModel;

    @Resource
    private Executor superAgentExecutor;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getName() {
        return "MemoryHook";
    }

    @Override
    public HookPosition[] getHookPositions() {
        return new HookPosition[]{HookPosition.BEFORE_MODEL, HookPosition.AFTER_MODEL};
    }

    @Override
    public CompletableFuture<Map<String, Object>> beforeModel(OverAllState state, RunnableConfig config) {
        Long userId = (Long) config.metadata("userId").orElse(null);
        if (userId == null) {
            return CompletableFuture.completedFuture(Map.of());
        }
        String userIdStr = userId.toString();
        try {
            Store store = config.store();
            Optional<StoreItem> itemOpt = store.getItem(List.of(USER_PROFILE_NAMESPACE), userIdStr);
            if (itemOpt.isPresent()) {
                Map<String, Object> rawProfile = itemOpt.get().getValue();
                if (rawProfile == null || rawProfile.isEmpty()) {
                    return CompletableFuture.completedFuture(Map.of());
                }
                long now = System.currentTimeMillis();
                long expireMillis = TimeUnit.DAYS.toMillis(PROFILE_EXPIRE_DAYS);
                Map<String, ProfileEntry> profileMap = parseRawProfileToMap(rawProfile);
                String userContext = profileMap.entrySet().stream()
                        .filter(entry -> (now - entry.getValue().getUpdatedAt()) < expireMillis)
                        .map(entry -> entry.getKey() + "=" + entry.getValue().getValue())
                        .collect(Collectors.joining(", ", "\n\n[当前用户画像上下文]\n", "\n请在后续回答中自然地参考这些信息。"));
                if (userContext.trim().equals("[当前用户画像上下文]请在后续回答中自然地参考这些信息。")) {
                    return CompletableFuture.completedFuture(Map.of());
                }
                List<Message> messages = (List<Message>) state.value("messages").orElse(new ArrayList<>());
                int systemMessageIndex = -1;
                for (int i = 0; i < messages.size(); i++) {
                    if (messages.get(i) instanceof SystemMessage) {
                        systemMessageIndex = i;
                        break;
                    }
                }
                if (systemMessageIndex >= 0) {
                    SystemMessage existingSystemMessage = (SystemMessage) messages.get(systemMessageIndex);
                    String existingText = existingSystemMessage.getText();
                    int markerIndex = existingText.indexOf("\n\n[当前用户画像上下文]\n");
                    if (markerIndex != -1) {
                        existingText = existingText.substring(0, markerIndex);
                    }
                    SystemMessage enhancedSystemMessage = new SystemMessage(existingText + userContext);
                    // 直接在原列表上替换，不通过返回值触发状态追加
                    messages.set(systemMessageIndex, enhancedSystemMessage);
                } else {
                    SystemMessage enhancedSystemMessage = new SystemMessage(userContext);
                    // 直接在原列表头部插入
                    messages.add(0, enhancedSystemMessage);
                }
                // 不返回 messages，避免框架当作增量追加导致记忆重复
                return CompletableFuture.completedFuture(Map.of());
            }
        } catch (Exception e) {
            log.error("加载用户画像失败, userId: {}", userId, e);
        }
        return CompletableFuture.completedFuture(Map.of());
    }

    @Override
    public CompletableFuture<Map<String, Object>> afterModel(OverAllState state, RunnableConfig config) {
        Long userId = (Long) config.metadata("userId").orElse(null);
        if (userId == null) {
            return CompletableFuture.completedFuture(Map.of());
        }

        List<Message> messages = (List<Message>) state.value("messages").orElse(new ArrayList<>());
        // 1. 只取当前轮次的用户输入作为提取源
        String currentUserInput = null;
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (messages.get(i) instanceof UserMessage) {
                currentUserInput = messages.get(i).getText();
                break;
            }
        }
        // 2. 防御性检查：没有输入或输入字数少于10字，本轮不做画像提取
        if (currentUserInput == null || currentUserInput.trim().length() < MIN_INPUT_LENGTH) {
            log.debug("用户输入过短或为空，跳过画像提取, userId: {}", userId);
            return CompletableFuture.completedFuture(Map.of());
        }
        // 异步执行：LLM 调用 + 持久化全部放到后台线程
        final String userIdStr = userId.toString();
        final String finalUserInput = currentUserInput;
        CompletableFuture.supplyAsync(() -> {
            try {
                Store store = config.store();
                Optional<StoreItem> itemOpt = store.getItem(List.of(USER_PROFILE_NAMESPACE), userIdStr);
                Map<String, ProfileEntry> profileMap = (itemOpt.isPresent() && itemOpt.get().getValue() != null)
                        ? parseRawProfileToMap(itemOpt.get().getValue())
                        : new HashMap<>();

                long now = System.currentTimeMillis();
                long expireMillis = TimeUnit.DAYS.toMillis(PROFILE_EXPIRE_DAYS);

                // 淘汰策略
                profileMap.entrySet().removeIf(entry -> (now - entry.getValue().getUpdatedAt()) > expireMillis);
                if (profileMap.size() > MAX_PROFILE_SIZE) {
                    Map<String, ProfileEntry> sortedMap = profileMap.entrySet().stream()
                            .sorted(Map.Entry.comparingByValue(Comparator.comparingLong(ProfileEntry::getUpdatedAt).reversed()))
                            .limit(MAX_PROFILE_SIZE)
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
                    profileMap.clear();
                    profileMap.putAll(sortedMap);
                }

                // 构建 Prompt
                String existingProfileDesc = profileMap.entrySet().stream()
                        .map(e -> e.getKey() + ": " + e.getValue().getValue())
                        .collect(Collectors.joining("\n"));
                String userContent = String.format(
                        "已有的用户画像数据如下：\n%s\n\n当前用户输入：\n%s\n\n请结合已有画像和当前用户输入，输出更新后的完整用户画像JSON。如果当前输入无有效偏好，直接输出原画像。",
                        existingProfileDesc.isEmpty() ? "无" : existingProfileDesc, finalUserInput
                );
                Prompt prompt = new Prompt(List.of(
                        new SystemMessage(EXTRACTION_SYSTEM_PROMPT),
                        new UserMessage(userContent)
                ));

                // LLM 调用（在后台线程中执行，不阻塞框架线程）
                ChatResponse chatResponse = zhiPuAiChatModel.call(prompt);
                String responseContent = chatResponse.getResult().getOutput().getText();

                // 解析与增量合并
                Map<String, String> deltaProfile = parseJsonToStringMap(responseContent);
                if (!deltaProfile.isEmpty()) {
                    deltaProfile.forEach((key, value) -> {
                        profileMap.put(key, new ProfileEntry(value, now));
                    });

                    Map<String, Object> rawProfileToSave = new HashMap<>();
                    profileMap.forEach((k, v) -> rawProfileToSave.put(k, objectMapper.convertValue(v, Map.class)));
                    StoreItem item = StoreItem.of(List.of(USER_PROFILE_NAMESPACE), userIdStr, rawProfileToSave);
                    store.putItem(item);
                    log.info("成功增量更新用户画像, userId: {}, 更新字段: {}", userId, deltaProfile.keySet());
                }
            } catch (Exception e) {
                log.error("提取或更新用户画像失败, userId: {}", userId, e);
            }
            return Map.<String, Object>of();
        }, superAgentExecutor);
        return CompletableFuture.completedFuture(Map.of());
    }

    /**
     * 解析带有时间戳的原始Map为内部结构
     */
    @SuppressWarnings("unchecked")
    private Map<String, ProfileEntry> parseRawProfileToMap(Map<String, Object> rawProfile) {
        Map<String, ProfileEntry> result = new HashMap<>();
        rawProfile.forEach((key, val) -> {
            try {
                if (val instanceof Map) {
                    result.put(key, objectMapper.convertValue(val, ProfileEntry.class));
                }
            } catch (Exception e) {
                log.warn("画像字段 {} 解析失败，将被淘汰", key);
            }
        });
        return result;
    }

    /**
     * 解析LLM返回的JSON为简单的String-Map
     */
    private Map<String, String> parseJsonToStringMap(String jsonStr) {
        try {
            String cleanJson = jsonStr.replaceAll("```json\\n?", "").replaceAll("```", "").trim();
            return objectMapper.readValue(cleanJson, new TypeReference<Map<String, String>>() {
            });
        } catch (Exception e) {
            log.warn("解析增量画像JSON失败，当作无更新处理。原始输出: {}", jsonStr);
            return Collections.emptyMap();
        }
    }

    /**
     * 画像条目元数据结构
     */
    @Setter
    @Getter
    public static class ProfileEntry {
        private String value;
        private long updatedAt;

        public ProfileEntry() {
        }

        public ProfileEntry(String value, long updatedAt) {
            this.value = value;
            this.updatedAt = updatedAt;
        }

    }
}