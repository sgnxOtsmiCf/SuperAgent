package cn.sgnxotsmicf.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import cn.sgnxotsmicf.common.result.Result;
import cn.sgnxotsmicf.common.vo.UserVo;
import cn.sgnxotsmicf.service.UserService;
import cn.sgnxotsmicf.service.strategy.login.LoginContext;
import cn.sgnxotsmicf.service.strategy.register.RegisterContext;
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
     * 统一登录入口
     *
     * @param loginType   登录类型：password / phone
     * @param username    用户名（账号密码登录时必填）
     * @param password    密码（账号密码登录时必填）
     * @param phone       手机号（手机号登录时必填）
     * @param captchaId   图片验证码ID
     * @param captchaCode 图片验证码值
     * @param verifyCode  短信验证码（手机号登录时必填）
     * @return SaTokenInfo
     */
    @Operation(summary = "用户登录-统一入口")
    @PostMapping("/login")
    public Result<SaTokenInfo> login(@RequestParam("loginType") String loginType,
                                     @RequestParam(value = "username", required = false) String username,
                                     @RequestParam(value = "password", required = false) String password,
                                     @RequestParam(value = "phone", required = false) String phone,
                                     @RequestParam("captchaId") String captchaId,
                                     @RequestParam("captchaCode") String captchaCode,
                                     @RequestParam(value = "verifyCode", required = false) String verifyCode) {
        LoginContext context = LoginContext.builder()
                .loginType(loginType)
                .username(username)
                .password(password)
                .phone(phone)
                .captchaId(captchaId)
                .captchaCode(captchaCode)
                .verifyCode(verifyCode)
                .build();
        return userService.login(context);
    }


    /**
     * 统一注册入口
     *
     * @param registerType 注册类型：password / phone
     * @param username     用户名（账号密码注册时必填）
     * @param password     密码（账号密码注册时必填）
     * @param phone        手机号（手机号注册时必填）
     * @param captchaId    图片验证码ID
     * @param captchaCode  图片验证码值
     * @param verifyCode   短信验证码（手机号注册时必填）
     * @return 注册结果
     */
    @Operation(summary = "用户注册-简单版")
    @PostMapping("/register")
    public Result<String> register(@RequestParam("registerType") String registerType,
                                   @RequestParam(value = "username", required = false) String username,
                                   @RequestParam(value = "password", required = false) String password,
                                   @RequestParam(value = "phone", required = false) String phone,
                                   @RequestParam("captchaId") String captchaId,
                                   @RequestParam("captchaCode") String captchaCode,
                                   @RequestParam(value = "verifyCode", required = false) String verifyCode) {
        RegisterContext context = RegisterContext.builder()
                .registerType(registerType)
                .username(username)
                .password(password)
                .phone(phone)
                .captchaId(captchaId)
                .captchaCode(captchaCode)
                .verifyCode(verifyCode)
                .build();
        return userService.register(context);
    }


    /**
     * 发送验证码
     * @param phone 手机号
     * @return SaTokenInfo对象
     */
    @Operation(summary = "发送验证码")
    @PostMapping("/LoginWithPhoneCodePre")
    public Result<SaTokenInfo> doLoginWithPhoneCodePre(@RequestParam("phone")String phone) {
        return userService.doLoginWithPhoneCodePre(phone);
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
     * minio图片头像文件上传
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
     * minio图片头像文件查询
     * @return 响应地址
     */
    @Operation(summary = "minio图片头像文件查询")
    @GetMapping("/getAvatarUrl")
    @SaCheckLogin
    public Result<String> fileSelect() {
        String fileUrl = userService.fileSelect();
        return Result.ok(fileUrl);
    }


    /**
     * 修改密码
     * @param password 密码
     * @param newPassword 新密码
     * @return 修改状态
     */
    @Operation(summary = "修改密码")
    @SaCheckLogin
    @PutMapping("/password")
    public Result<String> updatePassword(@RequestParam("password") String password, @RequestParam("newPassword") String newPassword) {
        return userService.updatePassword(password, newPassword);
    }


    @Operation(summary = "获取用户基本信息")
    @SaCheckLogin
    @GetMapping
    public Result<UserVo> getUserInfo() {
        return userService.getUserInfo();
    }

    @Operation(summary = "更新用户基本信息")
    @SaCheckLogin
    @PutMapping
    public Result<String> updateUserInfo(@RequestBody UserVo userVo) {
        return userService.updateUserInfo(userVo);
    }
}
