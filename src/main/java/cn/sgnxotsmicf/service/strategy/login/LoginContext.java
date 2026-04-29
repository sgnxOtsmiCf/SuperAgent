package cn.sgnxotsmicf.service.strategy.login;

import lombok.Builder;
import lombok.Data;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/28 9:42
 * @Version: 0.1
 * @Description: 登录上下文参数对象, 封装所有登录方式可能用到的参数，策略实现类按需取用。
 */

@Data
@Builder
public class LoginContext {

    /**
     * 登录类型：password / phone
     */
    private String loginType;

    /**
     * 用户名（账号密码登录）
     */
    private String username;

    /**
     * 密码（账号密码登录）
     */
    private String password;

    /**
     * 手机号（手机号登录）
     */
    private String phone;

    /**
     * 图片验证码ID（账号密码登录）
     */
    private String captchaId;

    /**
     * 图片验证码值（账号密码登录）
     */
    private String captchaCode;

    /**
     * 短信/手机验证码值（手机号登录）
     */
    private String verifyCode;
}
