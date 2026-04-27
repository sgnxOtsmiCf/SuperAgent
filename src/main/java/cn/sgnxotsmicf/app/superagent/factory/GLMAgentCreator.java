package cn.sgnxotsmicf.app.superagent.factory;

import cn.sgnxotsmicf.agentTool.ToolRegistry;
import cn.sgnxotsmicf.app.superagent.hook.HookRegistry;
import cn.sgnxotsmicf.app.superagent.interceptor.InterceptorRegistry;
import cn.sgnxotsmicf.common.model.ModelCommon;
import org.redisson.api.RedissonClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.zhipuai.ZhiPuAiChatModel;
import org.springframework.ai.zhipuai.ZhiPuAiChatOptions;
import org.springframework.ai.zhipuai.api.ZhiPuAiApi;
import org.springframework.stereotype.Component;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/27 17:22
 * @Version: 0.1
 * @Description:
 */
@Component
public class GLMAgentCreator extends AbstractReactAgentCreator{

    private final ZhiPuAiChatModel zhiPuAiChatModel;

    public GLMAgentCreator(ToolRegistry toolRegistry,
                           HookRegistry hookRegistry,
                           InterceptorRegistry interceptorRegistry,
                           RedissonClient redissonClient,
                           ZhiPuAiChatModel zhiPuAiChatModel) {
        super(toolRegistry, hookRegistry, interceptorRegistry, redissonClient);
        this.zhiPuAiChatModel = zhiPuAiChatModel;
    }

    @Override
    public ChatModel createChatModel(String modelId) {
        //先暂时这样设置
        return zhiPuAiChatModel;
    }

    @Override
    public ChatOptions createChatOptions(String modelId) {
        String lowerModelId = modelId != null ? modelId.toLowerCase() : "glm-4";
        return ZhiPuAiChatOptions.builder()
                .model(lowerModelId)
                .maxTokens(10000)
                .thinking(ZhiPuAiApi.ChatCompletionRequest.Thinking.enabled())
                .build();
    }

    @Override
    public String getSupportedModelIdPrefix() {
        return ModelCommon.GLM_PREFIX;
    }
}
