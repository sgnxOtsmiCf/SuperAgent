package cn.sgnxotsmicf.service.strategy.register;

import cn.sgnxotsmicf.common.result.Result;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/28 9:43
 * @Version: 0.1
 * @Description:
 */

public interface RegisterStrategy {

    /**
     * 判断是否支持指定的注册类型
     *
     * @param registerType 注册类型标识
     * @return true 表示当前策略支持该注册类型
     */
    boolean supports(String registerType);

    /**
     * 执行注册
     *
     * @param context 注册上下文参数
     * @return 注册结果
     */
    Result<String> register(RegisterContext context);
}
