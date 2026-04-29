package cn.sgnxotsmicf.service.strategy.register;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/28 9:43
 * @Version: 0.1
 * @Description:
 */

@Component
public class RegisterStrategyFactory {

    @Resource
    private List<RegisterStrategy> strategyList;

    /**
     * 根据注册类型获取对应策略
     *
     * @param registerType 注册类型
     * @return RegisterStrategy 对象
     */
    public RegisterStrategy getStrategy(String registerType) {
        return strategyList.stream()
                .filter(strategy -> strategy.supports(registerType))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("不支持的注册类型：" + registerType));
    }
}
