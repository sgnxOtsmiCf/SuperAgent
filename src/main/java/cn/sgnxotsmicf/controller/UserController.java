package cn.sgnxotsmicf.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import cn.sgnxotsmicf.common.result.Result;
import cn.sgnxotsmicf.common.vo.UserVo;
import cn.sgnxotsmicf.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/5 21:05
 * @Version: 1.0
 * @Description:
 */
@RestController
@RequestMapping("/user")
@Tag(name = "用户控制器")
public class UserController {

    private final UserService userService;


    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 用户登录-简单版
     * @param username 用户名
     * @param password 密码
     * @return SaTokenInfo
     */
    @Operation(summary = "用户登录-简单版")
    @PostMapping("/simpleLogin")
    public Result<SaTokenInfo> doLogin(@RequestParam("username")String username,
                                       @RequestParam("password")String password,
                                       @RequestParam("captchaId")String captchaId,
                                       @RequestParam("captchaCode")String captchaCode) {
        return userService.doLogin(username, password, captchaId, captchaCode);
    }

    /**
     * 发送验证码
     * @param phone 手机号
     * @return SaTokenInfo
     */
    @Operation(summary = "发送验证码")
    @PostMapping("/LoginWithPhoneCodePre")
    public Result<SaTokenInfo> doLoginWithPhoneCodePre(@RequestParam("phone")String phone) {
        return userService.doLoginWithPhoneCodePre(phone);
    }

    /**
     * 用户登录-手机号版
     * @param phone 手机号
     * @param verifyCode 验证码
     * @return 登录SaTokenInfo
     */
    @Operation(summary = "用户登录-手机号版")
    @PostMapping("/LoginWithPhoneCode")
    public Result<SaTokenInfo> doLoginWithPhoneCode(@RequestParam("phone")String phone, @RequestParam("verifyCode")String verifyCode) {
        return userService.doLoginWithPhoneCode(phone, verifyCode);
    }

    /*注册*/

    /**
     * 用户注册-简单版
     * @param username 用户名
     * @param password 密码
     * @return String
     */
    @Operation(summary = "用户注册-简单版")
    @PostMapping("/simpleRegister")
    public Result<String> register(@RequestParam("username") String username,@RequestParam("password") String password) {
        return userService.register(username,password);
    }

    /**
     * 注册前动态校验用户名是否已经存在
     * @param username 用户名
     * @return 验证结果
     */
    @Operation(summary = "注册前动态校验用户名是否已经存在")
    @PostMapping("/registerPre")
    public Result<String> registerPre(@RequestParam("username") String username) {
        return userService.registerPre(username);
    }

    /**
     * 用户退出登录
     * @return 响应状态
     */
    @Operation(summary = "用户退出登录")
    @SaCheckLogin
    @GetMapping("/logout")
    public Result<String> logout(){
        StpUtil.logout();
        return Result.ok();
    }

    /**
     *
     * @param file 用户图像
     * @return 响应地址
     */
    @Operation(summary = "minio图片头像文件上传")
    @PostMapping("/fileUpload")
    @SaCheckLogin
    public Result<String> fileUpload(@RequestParam("file") MultipartFile file) {
        String fileUrl = userService.uploadFile(file);
        return Result.ok(fileUrl);
    }

    /**
     *
     * @param userId 用户id
     * @return 响应地址
     */
    @Operation(summary = "minio图片头像文件查询")
    @GetMapping("/getAvatarUrl")
    @SaCheckLogin
    public Result<String> fileSelect(@RequestParam("userId") Long userId) {
        String fileUrl = userService.fileSelect(userId);
        return Result.ok(fileUrl);
    }


    /**
     * 修改密码
     * @param userId 用户id
     * @param password 密码
     * @param newPassword 新密码
     * @return 修改状态
     */
    @Operation(summary = "修改密码")
    @SaCheckLogin
    @PutMapping("/password")
    public Result<String> updatePassword(@RequestParam("userId") Long userId, @RequestParam("password") String password, @RequestParam("newPassword") String newPassword) {
        return userService.updatePassword(userId, password, newPassword);
    }


    @Operation(summary = "获取用户基本信息")
    @SaCheckLogin
    @GetMapping
    public Result<UserVo> getUserInfo(@RequestParam("userId") Long userId) {
        return userService.getUserInfo(userId);
    }

    @Operation(summary = "更新用户基本信息")
    @SaCheckLogin
    @PutMapping
    public Result<String> updateUserInfo(@RequestBody UserVo userVo) {
        return userService.updateUserInfo(userVo);
    }
}
