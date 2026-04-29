package cn.sgnxotsmicf.app.superagent.factory;

import cn.sgnxotsmicf.agentTool.ToolRegistry;
import cn.sgnxotsmicf.app.superagent.hook.HookRegistry;
import cn.sgnxotsmicf.app.superagent.interceptor.InterceptorRegistry;
import cn.sgnxotsmicf.common.model.ModelCommon;
import cn.sgnxotsmicf.common.vo.ChatRequest;
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
    public ChatModel createChatModel(ChatRequest request) {
        //先暂时这样设置
        return zhiPuAiChatModel;
    }

    @Override
    public ChatOptions createChatOptions(ChatRequest request) {
        String modelId = request.getModelId();
        String lowerModelId = modelId != null ? modelId.toLowerCase() : "glm-4";
        return ZhiPuAiChatOptions.builder()
                .model(lowerModelId)
                .maxTokens(request.getMaxTokens().intValue())
                .temperature(request.getTemperature())
                .topP(request.getTopP())
                .thinking(request.getEnableThinking() != null ?ZhiPuAiApi.ChatCompletionRequest.Thinking.enabled() : null)
                .build();
    }

    @Override
    public String getSupportedModelIdPrefix() {
        return ModelCommon.GLM_PREFIX;
    }
}
