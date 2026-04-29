package cn.sgnxotsmicf.service.strategy.register;

import lombok.Builder;
import lombok.Data;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/28 9:42
 * @Version: 0.1
 * @Description:
 */

@Data
@Builder
public class RegisterContext {

    /**
     * 注册类型：password / phone
     */
    private String registerType;

    /**
     * 用户名（账号密码注册）
     */
    private String username;

    /**
     * 密码（账号密码注册）
     */
    private String password;

    /**
     * 手机号（手机号注册）
     */
    private String phone;

    /**
     * 图片验证码ID
     */
    private String captchaId;

    /**
     * 图片验证码值
     */
    private String captchaCode;

    /**
     * 短信验证码（手机号注册）
     */
    private String verifyCode;
}
