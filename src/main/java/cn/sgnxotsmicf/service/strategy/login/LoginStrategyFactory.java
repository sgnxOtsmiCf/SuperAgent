package cn.sgnxotsmicf.service.strategy.login;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/28 9:42
 * @Version: 0.1
 * @Description: 登录策略工厂
 */

@Component
public class LoginStrategyFactory {

    @Resource
    private List<LoginStrategy> strategyList;

    /**
     * 根据登录类型获取对应策略
     *
     * @param loginType 登录类型
     * @return LoginStrategy 对象
     */
    public LoginStrategy getStrategy(String loginType) {
        return strategyList.stream()
                .filter(strategy -> strategy.supports(loginType))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("不支持的登录类型：" + loginType));
    }
}
