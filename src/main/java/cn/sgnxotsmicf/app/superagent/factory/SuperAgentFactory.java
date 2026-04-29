package cn.sgnxotsmicf.app.superagent.factory;

import cn.sgnxotsmicf.chatMemory.RedissonStore;
import cn.sgnxotsmicf.common.vo.ChatRequest;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.savers.redis.RedisSaver;
import com.alibaba.cloud.ai.graph.store.Store;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/27 16:23
 * @Version: 0.1
 * @Description:
 */

@Slf4j
@Configuration
public class SuperAgentFactory {

    private final Map<String, AbstractReactAgentCreator> prefixRouter;

    private final AbstractReactAgentCreator defaultCreator;

    private final RedissonClient redissonClient;


    public SuperAgentFactory(ObjectProvider<AbstractReactAgentCreator> creatorProvider, RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
        List<AbstractReactAgentCreator> creators = creatorProvider.stream().toList();

        if (CollectionUtils.isEmpty(creators)) {
            throw new IllegalStateException("未找到任何 AbstractReactAgentCreator 实现类，请检查是否有具体的 Agent 创建器被 Spring 管理");
        }

        Map<String, AbstractReactAgentCreator> tempRouter = new HashMap<>();

        for (AbstractReactAgentCreator creator : creators) {
            String prefix = creator.getSupportedModelIdPrefix();

            if (!StringUtils.hasText(prefix)) {
                throw new IllegalStateException(
                        creator.getClass().getName() + " 的 getSupportedModelIdPrefix() 返回空值，必须提供有效的前缀"
                );
            }

            // 统一转小写，避免大小写问题
            String normalizedPrefix = prefix.trim().toLowerCase();

            // 前缀冲突检测：不允许两个 Creator 注册同一个前缀
            if (tempRouter.containsKey(normalizedPrefix)) {
                AbstractReactAgentCreator existing = tempRouter.get(normalizedPrefix);
                throw new IllegalStateException(String.format(
                        "AgentCreator 前缀冲突: [%s] 已被 [%s] 注册，[%s] 无法重复注册",
                        normalizedPrefix,
                        existing.getClass().getSimpleName(),
                        creator.getClass().getSimpleName()
                ));
            }

            tempRouter.put(normalizedPrefix, creator);
        }

        this.prefixRouter = Collections.unmodifiableMap(tempRouter);
        //兜底
        this.defaultCreator = creators.getFirst();
    }


    /**
     * 创建 Agent，根据 modelId 选择对应的模型
     *
     * @param toolContextConfig 工具上下文配置
     * @param request           ChatRequest 核心请求
     * @return ReactAgent ReactAgent的实例
     */
    public ReactAgent createAgent(Map<String, Object> toolContextConfig, ChatRequest request) {
        String modelId = request.getModelId();
        System.out.println("modelId: " + modelId);
        AbstractReactAgentCreator creator = resolveCreator(modelId);
        return creator.createAgent(toolContextConfig, request);
    }


    /**
     * 解析 modelId 获取对应的 Creator
     * 策略：先按前缀匹配，匹配失败则使用兜底 Creator（或抛异常）
     */
    private AbstractReactAgentCreator resolveCreator(String modelId) {
        if (!StringUtils.hasText(modelId)) {
            throw new IllegalArgumentException("modelId 不能为空");
        }

        // 提取前缀：deepseek-chat -> deepseek
        String normalizedModelId = modelId.trim().toLowerCase();

        AbstractReactAgentCreator creator = prefixRouter
                .entrySet()
                .stream()
                .filter(entry -> normalizedModelId.startsWith(entry.getKey())).toList().getFirst().getValue();
        //AbstractReactAgentCreator creator = prefixRouter.get(prefix);

        if (creator != null) {
            return creator;
        }
        // 前缀未命中，尝试完整精确匹配（如 modelId 本身就是前缀）
        creator = prefixRouter.get(normalizedModelId);
        if (creator != null) {
            return creator;
        }
        log.warn("进入兜底,使用{}类模型", defaultCreator.getSupportedModelIdPrefix());

        return defaultCreator;
    }


    /**
     * 获取当前注册的所有前缀
     */
    public Map<String, String> getRegisteredPrefixes() {
        return prefixRouter.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().getClass().getSimpleName()
                ));
    }


    /**
     * 创建 RedisSaver
     */
    public RedisSaver buildRedisSaver() {
        return RedisSaver.builder()
                .redisson(redissonClient)
                .build();
    }

    /**
     * 创建 RedisStore
     */
    public Store buildRedisStore() {
        return new RedissonStore(redissonClient, "spring-ai-graph:store:");
    }
}