package cn.sgnxotsmicf.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.sgnxotsmicf.common.result.Result;
import cn.sgnxotsmicf.common.vo.UserProfileVo;
import cn.sgnxotsmicf.service.UserProfileService;
import cn.sgnxotsmicf.service.impl.UserProfileServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;



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
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;


    @Operation(summary = "获取用户画像")
    @GetMapping("/{userId}")
    @SaCheckLogin
    public Result<UserProfileVo> getUserProfile(@PathVariable Long userId) {
        return userProfileService.getUserProfile(userId);
    }


    @Operation(summary = "更新用户画像维度（全量覆盖该维度的所有条目）")
    @PutMapping("/{userId}")
    @SaCheckLogin
    public Result<UserProfileVo> updateUserProfile(@PathVariable Long userId, @RequestBody UserProfileServiceImpl.UpdateProfileRequest request) {
        return userProfileService.updateUserProfile(userId, request);
    }


    @Operation(summary = "删除用户画像维度或条目")
    @DeleteMapping("/{userId}")
    @SaCheckLogin
    public Result<UserProfileVo> deleteUserProfile(@PathVariable Long userId,
                                                   @RequestParam String dimension,
                                                   @RequestParam(required = false) String value){
        return userProfileService.deleteUserProfile(userId, dimension, value);
    }

}