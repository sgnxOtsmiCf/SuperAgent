package cn.sgnxotsmicf.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.sgnxotsmicf.app.superagent.SuperAgentFactory;
import cn.sgnxotsmicf.app.superagent.hook.model.MemoryHook;
import cn.sgnxotsmicf.common.result.Result;
import cn.sgnxotsmicf.common.vo.ProfileItemVo;
import cn.sgnxotsmicf.common.vo.UserProfileVo;
import com.alibaba.cloud.ai.graph.store.Store;
import com.alibaba.cloud.ai.graph.store.StoreItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;


/**
 * @Author: lixiang
 * @CreateDate: 2026/4/20 14:22
 * @Version: 1.0
 * @Description: 用户画像控制器
 */

@Tag(name = "用户画像控制器", description = "用户画像")
@RestController
@RequestMapping("/profile")
@Slf4j
public class UserProfileController {

    private static final String USER_PROFILE_NAMESPACE = "user_profiles";

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    @Resource
    private SuperAgentFactory superAgentFactory;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Operation(summary = "获取用户画像")
    @GetMapping("/{userId}")
    @SaCheckLogin
    public Result<UserProfileVo> getUserProfile(@PathVariable Long userId) {
        UserProfileVo vo = new UserProfileVo();
        vo.setUserId(userId);
        vo.setProfiles(new ArrayList<>());
        try {
            String userIdStr = userId.toString();
            Store store = superAgentFactory.buildRedisStore();
            Optional<StoreItem> itemOpt = store.getItem(List.of(USER_PROFILE_NAMESPACE), userIdStr);
            if (itemOpt.isPresent() && itemOpt.get().getValue() != null) {
                Map<String, Object> rawProfile = itemOpt.get().getValue();
                Map<String, MemoryHook.ProfileEntry> profileMap = parseRawProfileToMap(rawProfile);
                // 转换为 VO 列表
                List<ProfileItemVo> itemVOList = profileMap.entrySet().stream()
                        .map(entry -> {
                            ProfileItemVo itemVO = new ProfileItemVo();
                            itemVO.setKey(entry.getKey());
                            itemVO.setValue(entry.getValue().getValue());
                            itemVO.setUpdatedAt(entry.getValue().getUpdatedAt());
                            // 将时间戳转为可读字符串
                            itemVO.setUpdatedTimeStr(FORMATTER.format(Instant.ofEpochMilli(entry.getValue().getUpdatedAt())));
                            return itemVO;
                        })
                        // 按更新时间倒序排列，最新的在最前面
                        .sorted(Comparator.comparingLong(ProfileItemVo::getUpdatedAt).reversed())
                        .collect(Collectors.toList());
                vo.setProfiles(itemVOList);
            }
        } catch (Exception e) {
            log.error("查询用户画像失败, userId: {}", userId, e);
        }
        return Result.ok(vo);
    }

    /**
     * 解析带有时间戳的原始Map为内部结构 (逻辑与 MemoryHook 保持一致)
     */
    private Map<String, MemoryHook.ProfileEntry> parseRawProfileToMap(Map<String, Object> rawProfile) {
        Map<String, MemoryHook.ProfileEntry> result = new HashMap<>();
        rawProfile.forEach((key, val) -> {
            try {
                if (val instanceof Map) {
                    result.put(key, objectMapper.convertValue(val, MemoryHook.ProfileEntry.class));
                }
            } catch (Exception e) {
                log.warn("画像字段 {} 解析失败，跳过", key);
            }
        });
        return result;
    }
}