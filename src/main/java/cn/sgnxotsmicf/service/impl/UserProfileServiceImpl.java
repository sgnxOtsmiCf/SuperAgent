package cn.sgnxotsmicf.service.impl;

import cn.sgnxotsmicf.app.superagent.factory.SuperAgentFactory;
import cn.sgnxotsmicf.app.superagent.hook.model.MemoryHook;
import cn.sgnxotsmicf.common.result.Result;
import cn.sgnxotsmicf.common.result.ResultCodeEnum;
import cn.sgnxotsmicf.common.tools.ServiceUtil;
import cn.sgnxotsmicf.common.vo.ProfileItemVo;
import cn.sgnxotsmicf.common.vo.UserProfileVo;
import cn.sgnxotsmicf.service.UserProfileService;
import com.alibaba.cloud.ai.graph.store.Store;
import com.alibaba.cloud.ai.graph.store.StoreItem;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/28 21:18
 * @Version: 1.0
 * @Description:
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class UserProfileServiceImpl implements UserProfileService {

    private static final String USER_PROFILE_NAMESPACE = "user_profiles";

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final SuperAgentFactory superAgentFactory;

    private final ServiceUtil serviceUtil;

    @Override
    public Result<UserProfileVo> getUserProfile() {
        Long userId = serviceUtil.getUserId();
        UserProfileVo vo = new UserProfileVo();
        vo.setUserId(userId);
        vo.setProfiles(new ArrayList<>());
        try {
            String userIdStr = userId.toString();
            Store store = superAgentFactory.buildRedisStore();
            Optional<StoreItem> itemOpt = store.getItem(List.of(USER_PROFILE_NAMESPACE), userIdStr);
            if (itemOpt.isPresent() && itemOpt.get().getValue() != null) {
                Map<String, Object> rawProfile = itemOpt.get().getValue();
                Map<String, List<MemoryHook.ProfileEntry>> profileMap = parseRawProfileToMap(rawProfile);
                List<ProfileItemVo> itemVOList = profileMap.entrySet().stream()
                        .flatMap(entry -> entry.getValue().stream()
                                .map(profileEntry -> {
                                    ProfileItemVo itemVO = new ProfileItemVo();
                                    itemVO.setKey(entry.getKey());
                                    itemVO.setValue(profileEntry.getValue());
                                    itemVO.setUpdatedAt(profileEntry.getUpdatedAt());
                                    itemVO.setUpdatedTimeStr(FORMATTER.format(Instant.ofEpochMilli(profileEntry.getUpdatedAt())));
                                    return itemVO;
                                })
                        )
                        .sorted(Comparator.comparingLong(ProfileItemVo::getUpdatedAt).reversed())
                        .collect(Collectors.toList());
                vo.setProfiles(itemVOList);
            }
        } catch (Exception e) {
            log.error("查询用户画像失败, userId: {}", userId, e);
        }
        return Result.ok(vo);
    }

    @Override
    public Result<UserProfileVo> updateUserProfile(UserProfileServiceImpl.UpdateProfileRequest request) {
        Long userId = serviceUtil.getUserId();
        if (request.getDimension() == null || request.getDimension().isBlank()) {
            return Result.build(null, ResultCodeEnum.FAIL.getCode(), "维度和值不能为空");
        }
        try {
            String userIdStr = userId.toString();
            Store store = superAgentFactory.buildRedisStore();
            Optional<StoreItem> itemOpt = store.getItem(List.of(USER_PROFILE_NAMESPACE), userIdStr);

            Map<String, List<MemoryHook.ProfileEntry>> profileMap = (itemOpt.isPresent() && itemOpt.get().getValue() != null)
                    ? parseRawProfileToMap(itemOpt.get().getValue())
                    : new LinkedHashMap<>();

            long now = System.currentTimeMillis();
            String dimension = request.getDimension().trim();

            if (request.getValues() == null || request.getValues().isEmpty()) {
                // 值为空则删除整个维度
                profileMap.remove(dimension);
            } else {
                List<MemoryHook.ProfileEntry> existingEntries = profileMap.getOrDefault(dimension, new ArrayList<>());
                Set<String> existingValueSet = existingEntries.stream()
                        .map(MemoryHook.ProfileEntry::getValue)
                        .collect(Collectors.toSet());

                List<MemoryHook.ProfileEntry> mergedEntries = new ArrayList<>();
                for (String newValue : request.getValues()) {
                    String trimmed = newValue.trim();
                    if (trimmed.isEmpty()) continue;
                    if (existingValueSet.contains(trimmed)) {
                        existingEntries.stream()
                                .filter(e -> e.getValue().equals(trimmed))
                                .findFirst()
                                .ifPresent(mergedEntries::add);
                    } else {
                        mergedEntries.add(new MemoryHook.ProfileEntry(trimmed, now));
                    }
                }
                if (mergedEntries.isEmpty()) {
                    profileMap.remove(dimension);
                } else {
                    profileMap.put(dimension, mergedEntries);
                }
            }

            Map<String, Object> rawProfileToSave = new HashMap<>(profileMap);
            StoreItem item = StoreItem.of(List.of(USER_PROFILE_NAMESPACE), userIdStr, rawProfileToSave);
            store.putItem(item);
            log.info("手动更新用户画像成功, userId: {}, dimension: {}", userId, dimension);

            return getUserProfile();
        } catch (Exception e) {
            log.error("更新用户画像失败, userId: {}", userId, e);
            return Result.build(null, ResultCodeEnum.FAIL.getCode(), "更新用户画像失败: " + e.getMessage());
        }
    }

    @Override
    public Result<UserProfileVo> deleteUserProfile(String dimension, String value) {
        Long userId = serviceUtil.getUserId();
        try {
            String userIdStr = userId.toString();
            Store store = superAgentFactory.buildRedisStore();
            Optional<StoreItem> itemOpt = store.getItem(List.of(USER_PROFILE_NAMESPACE), userIdStr);
            if (itemOpt.isEmpty() || itemOpt.get().getValue() == null) {
                return Result.build(null, ResultCodeEnum.FAIL.getCode(), "用户画像不存在");
            }

            Map<String, List<MemoryHook.ProfileEntry>> profileMap = parseRawProfileToMap(itemOpt.get().getValue());
            String dim = dimension.trim();

            if (!profileMap.containsKey(dim)) {
                return Result.build(null, ResultCodeEnum.FAIL.getCode(), "维度不存在: " + dim);
            }

            if (value == null || value.isBlank()) {
                profileMap.remove(dim);
                log.info("删除用户画像维度, userId: {}, dimension: {}", userId, dim);
            } else {
                String val = value.trim();
                List<MemoryHook.ProfileEntry> entries = profileMap.get(dim);
                if (entries != null) {
                    entries.removeIf(e -> e.getValue().equals(val));
                    if (entries.isEmpty()) {
                        profileMap.remove(dim);
                    }
                }
                log.info("删除用户画像条目, userId: {}, dimension: {}, value: {}", userId, dim, val);
            }

            Map<String, Object> rawProfileToSave = new HashMap<>(profileMap);
            StoreItem item = StoreItem.of(List.of(USER_PROFILE_NAMESPACE), userIdStr, rawProfileToSave);
            store.putItem(item);

            return getUserProfile();
        } catch (Exception e) {
            log.error("删除用户画像失败, userId: {}", userId, e);
            return Result.build(null, ResultCodeEnum.FAIL.getCode(), "删除用户画像失败: " + e.getMessage());
        }
    }


    /**
     * 解析原始存储Map为 Map<String, List<ProfileEntry>>（与 MemoryHook 新数据结构保持一致）
     */
    @SuppressWarnings("unchecked")
    private Map<String, List<MemoryHook.ProfileEntry>> parseRawProfileToMap(Map<String, Object> rawProfile) {
        Map<String, List<MemoryHook.ProfileEntry>> result = new LinkedHashMap<>();
        rawProfile.forEach((key, val) -> {
            try {
                if (val instanceof List) {
                    List<MemoryHook.ProfileEntry> entries = objectMapper.convertValue(val,
                            new TypeReference<List<MemoryHook.ProfileEntry>>() {
                            });
                    if (entries != null) {
                        result.put(key, entries);
                    }
                }
            } catch (Exception e) {
                log.warn("画像维度 {} 解析失败，跳过", key);
            }
        });
        return result;
    }

    @Data
    public static class UpdateProfileRequest {
        private String dimension;
        private List<String> values;
    }
}
