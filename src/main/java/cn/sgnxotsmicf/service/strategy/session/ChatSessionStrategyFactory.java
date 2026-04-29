package cn.sgnxotsmicf.service.strategy.session;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/17 11:44
 * @Version: 1.0
 * @Description:
 */

@Component
public class ChatSessionStrategyFactory {

    @Resource
    private List<ChatSessionStrategy> strategyList;


    /**
     * 根据agentId获取对应的会话策略
     * @param agentId 智能体id
     * @return ChatSessionStrategy对象
     */
    public ChatSessionStrategy getStrategy(Long agentId) {
        return strategyList.stream()
                .filter(strategy -> strategy.supports(agentId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("不支持的Agent类型：" + agentId));
    }
}
