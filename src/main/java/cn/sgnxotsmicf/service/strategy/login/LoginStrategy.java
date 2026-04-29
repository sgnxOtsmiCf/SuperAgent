package cn.sgnxotsmicf.service.strategy.login;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.sgnxotsmicf.common.result.Result;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/28 9:42
 * @Version: 0.1
 * @Description: 用户登录策略接口 定义不同登录方式的标准操作。。
 */

public interface LoginStrategy {

    /**
     * 判断是否支持指定的登录类型
     *
     * @param loginType 登录类型标识
     * @return true 表示当前策略支持该登录类型
     */
    boolean supports(String loginType);

    /**
     * 执行登录
     *
     * @param context 登录上下文参数
     * @return 登录结果，包含 SaTokenInfo
     */
    Result<SaTokenInfo> login(LoginContext context);
}
