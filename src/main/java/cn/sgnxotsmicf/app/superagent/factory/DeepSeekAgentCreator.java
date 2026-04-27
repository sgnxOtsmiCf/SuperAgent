package cn.sgnxotsmicf.app.superagent.factory;

import cn.sgnxotsmicf.agentTool.ToolRegistry;
import cn.sgnxotsmicf.app.superagent.hook.HookRegistry;
import cn.sgnxotsmicf.app.superagent.interceptor.InterceptorRegistry;
import cn.sgnxotsmicf.common.model.ModelCommon;
import org.redisson.api.RedissonClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.deepseek.DeepSeekChatOptions;
import org.springframework.stereotype.Component;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/27 17:20
 * @Version: 0.1
 * @Description:
 */

@Component
public class DeepSeekAgentCreator extends AbstractReactAgentCreator{

    private final DeepSeekChatModel deepSeekChatModel;

    public DeepSeekAgentCreator(ToolRegistry toolRegistry,
                                HookRegistry hookRegistry,
                                InterceptorRegistry interceptorRegistry,
                                RedissonClient redissonClient,
                                DeepSeekChatModel deepSeekChatModel) {
        super(toolRegistry, hookRegistry, interceptorRegistry, redissonClient);
        this.deepSeekChatModel = deepSeekChatModel;
    }

    @Override
    public ChatModel createChatModel(String modelId) {
        //先暂时这样设置
        return deepSeekChatModel;
    }

    @Override
    public ChatOptions createChatOptions(String modelId) {
        String lowerModelId = modelId != null ? modelId.toLowerCase() : "deepseek-chat";
        return DeepSeekChatOptions.builder()
                .model(lowerModelId)
                .maxTokens(10000)
                .build();
    }

    @Override
    public String getSupportedModelIdPrefix() {
        return ModelCommon.DEEPSEEK_PREFIX;
    }
}
