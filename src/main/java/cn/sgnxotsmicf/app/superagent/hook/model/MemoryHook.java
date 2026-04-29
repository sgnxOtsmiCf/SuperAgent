package cn.sgnxotsmicf.app.superagent.hook.model;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import com.alibaba.cloud.ai.graph.agent.hook.ModelHook;
import com.alibaba.cloud.ai.graph.store.Store;
import com.alibaba.cloud.ai.graph.store.StoreItem;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
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
 * @Version: 5.0
 * @Description: 动态用户画像记忆Hook，基于LLM进行增量事实提取与合并，支持时间衰减与容量淘汰。
 *               每个维度的值为原子条目列表(List<ProfileEntry>)，仅真正变更的条目刷新过期时间。
 */
@Component
@Slf4j
@HookPositions(value = {HookPosition.BEFORE_MODEL, HookPosition.AFTER_MODEL})
public class MemoryHook extends ModelHook {

    private static final String USER_PROFILE_NAMESPACE = "user_profiles";

    private static final int MAX_PROFILE_SIZE = 50;

    private static final long PROFILE_EXPIRE_DAYS = 30;

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
                Map<String, List<ProfileEntry>> profileMap = parseRawProfileToMap(rawProfile);
                String userContext = profileMap.entrySet().stream()
                        .map(entry -> {
                            String values = entry.getValue().stream()
                                    .filter(e -> (now - e.getUpdatedAt()) < expireMillis)
                                    .map(ProfileEntry::getValue)
                                    .collect(Collectors.joining(", "));
                            return values.isEmpty() ? null : entry.getKey() + "=" + values;
                        })
                        .filter(Objects::nonNull)
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
                    messages.set(systemMessageIndex, enhancedSystemMessage);
                } else {
                    SystemMessage enhancedSystemMessage = new SystemMessage(userContext);
                    messages.addFirst(enhancedSystemMessage);
                }
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
        String currentUserInput = null;
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (messages.get(i) instanceof UserMessage) {
                currentUserInput = messages.get(i).getText();
                break;
            }
        }
        if (currentUserInput == null || currentUserInput.trim().length() < MIN_INPUT_LENGTH) {
            log.debug("用户输入过短或为空，跳过画像提取, userId: {}", userId);
            return CompletableFuture.completedFuture(Map.of());
        }
        final String userIdStr = userId.toString();
        final String finalUserInput = currentUserInput;
        CompletableFuture.supplyAsync(() -> {
            try {
                Store store = config.store();
                Optional<StoreItem> itemOpt = store.getItem(List.of(USER_PROFILE_NAMESPACE), userIdStr);
                Map<String, List<ProfileEntry>> profileMap = (itemOpt.isPresent() && itemOpt.get().getValue() != null)
                        ? parseRawProfileToMap(itemOpt.get().getValue())
                        : new HashMap<>();

                long now = System.currentTimeMillis();
                long expireMillis = TimeUnit.DAYS.toMillis(PROFILE_EXPIRE_DAYS);

                expireStaleEntries(profileMap, now, expireMillis);
                enforceCapacityLimit(profileMap);

                String existingProfileDesc = profileMap.entrySet().stream()
                        .map(e -> e.getKey() + ": " + e.getValue().stream()
                                .map(ProfileEntry::getValue)
                                .collect(Collectors.joining(", ")))
                        .collect(Collectors.joining("\n"));
                String userContent = String.format(
                        "已有的用户画像数据如下：\n%s\n\n当前用户输入：\n%s\n\n请结合已有画像和当前用户输入，输出更新后的完整用户画像JSON。如果当前输入无有效偏好，直接输出原画像。",
                        existingProfileDesc.isEmpty() ? "无" : existingProfileDesc, finalUserInput
                );
                Prompt prompt = new Prompt(List.of(
                        new SystemMessage(EXTRACTION_SYSTEM_PROMPT),
                        new UserMessage(userContent)
                ));

                ChatResponse chatResponse = zhiPuAiChatModel.call(prompt);
                String responseContent = chatResponse.getResult().getOutput().getText();

                Map<String, String> llmResult = parseJsonToStringMap(responseContent);
                if (!llmResult.isEmpty()) {
                    boolean hasChanges = mergeFineGrained(profileMap, llmResult, now);
                    if (hasChanges) {
                        // 合并后清理空维度
                        profileMap.entrySet().removeIf(entry -> entry.getValue().isEmpty());
                        Map<String, Object> rawProfileToSave = new HashMap<>(profileMap);
                        StoreItem item = StoreItem.of(List.of(USER_PROFILE_NAMESPACE), userIdStr, rawProfileToSave);
                        store.putItem(item);
                        log.info("成功增量更新用户画像, userId: {}, 涉及维度: {}", userId, llmResult.keySet());
                    } else {
                        log.debug("用户画像无实质性变更, userId: {}", userId);
                    }
                }
            } catch (Exception e) {
                log.error("提取或更新用户画像失败, userId: {}", userId, e);
            }
            return Map.<String, Object>of();
        }, superAgentExecutor);
        return CompletableFuture.completedFuture(Map.of());
    }

    private void expireStaleEntries(Map<String, List<ProfileEntry>> profileMap, long now, long expireMillis) {
        profileMap.values().forEach(list -> list.removeIf(e -> (now - e.getUpdatedAt()) > expireMillis));
        profileMap.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    private void enforceCapacityLimit(Map<String, List<ProfileEntry>> profileMap) {
        if (profileMap.size() <= MAX_PROFILE_SIZE) {
            return;
        }
        Map<String, List<ProfileEntry>> sortedMap = profileMap.entrySet().stream()
                .sorted(Map.Entry.<String, List<ProfileEntry>>comparingByValue(
                                Comparator.comparingLong(list -> list.stream()
                                        .mapToLong(ProfileEntry::getUpdatedAt)
                                        .max()
                                        .orElse(0L)))
                        .reversed())
                .limit(MAX_PROFILE_SIZE)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        profileMap.clear();
        profileMap.putAll(sortedMap);
    }

    /**
     * 细粒度合并：将LLM返回的字符串值拆分后与已有条目逐项对比，
     * 仅新增的条目使用当前时间戳，已有不变条目保留原有时间戳。
     *
     * @return true 表示画像发生了实质变更
     */
    private boolean mergeFineGrained(Map<String, List<ProfileEntry>> profileMap, Map<String, String> llmResult, long now) {
        boolean hasChanges = false;
        for (Map.Entry<String, String> deltaEntry : llmResult.entrySet()) {
            String dimension = deltaEntry.getKey();
            String newValueStr = deltaEntry.getValue();
            List<String> newItems = splitValues(newValueStr);

            List<ProfileEntry> existingItems = profileMap.getOrDefault(dimension, new ArrayList<>());
            Set<String> existingValueSet = existingItems.stream()
                    .map(ProfileEntry::getValue)
                    .collect(Collectors.toSet());

            boolean dimensionChanged = false;
            List<ProfileEntry> mergedItems = new ArrayList<>();
            for (String newItem : newItems) {
                if (existingValueSet.contains(newItem)) {
                    // 值未变，保留原有条目（含原有时间戳）
                    existingItems.stream()
                            .filter(e -> e.getValue().equals(newItem))
                            .findFirst()
                            .ifPresent(mergedItems::add);
                } else {
                    // 新增条目，使用当前时间戳
                    mergedItems.add(new ProfileEntry(newItem, now));
                    dimensionChanged = true;
                }
            }
            // 检测条目是否被删除
            if (mergedItems.size() != existingItems.size()) {
                dimensionChanged = true;
            }

            if (dimensionChanged || !profileMap.containsKey(dimension)) {
                profileMap.put(dimension, mergedItems);
                hasChanges = true;
            }
        }
        return hasChanges;
    }

    /**
     * 将逗号分隔的字符串拆分为原子值列表（支持中英文逗号、顿号）
     */
    private List<String> splitValues(String value) {
        if (value == null || value.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(value.split("[，,、]"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * 解析原始存储Map为 Map<String, List<ProfileEntry>>
     */
    private Map<String, List<ProfileEntry>> parseRawProfileToMap(Map<String, Object> rawProfile) {
        Map<String, List<ProfileEntry>> result = new LinkedHashMap<>();
        rawProfile.forEach((key, val) -> {
            try {
                if (val instanceof List) {
                    List<ProfileEntry> entries = objectMapper.convertValue(val, new TypeReference<List<ProfileEntry>>() {});
                    if (entries != null) {
                        result.put(key, entries);
                    }
                }
            } catch (Exception e) {
                log.warn("画像维度 {} 解析失败，将被淘汰", key);
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
     * 画像原子条目：单个偏好/属性值 + 独立过期时间，以value判断相等性
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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ProfileEntry that)) return false;
            return Objects.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
    }

    /* 目前存储的格式
     * {
     * "namespace":["user_profiles"],
     * "key":"2",
     * "value":{
     *     "偏好领域":[
     *         {"value":"中国古代历史","updatedAt":1777355210755},
     *         {"value":"文言文","updatedAt":1777355210755}
     *     ],
     *     "求职方向":[
     *         {"value":"Java后端开发实习","updatedAt":1777355210755}
     *     ],
     *     "技术偏好":[
     *         {"value":"Java","updatedAt":1777355210755},
     *         {"value":"caveman中等模式","updatedAt":1777355210755},
     *         {"value":"Python","updatedAt":1777355210755},
     *         {"value":"C++","updatedAt":1777355210755},
     *         {"value":"并发编程","updatedAt":1777355210755}
     *     ]
     * },
     * "createdAt":1777355218112,
     * "updatedAt":1777355218112
     * }
     */
}
