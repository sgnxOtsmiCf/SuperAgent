package cn.sgnxotsmicf.app.superagent.factory;

import cn.sgnxotsmicf.agentTool.ToolRegistry;
import cn.sgnxotsmicf.app.superagent.hook.HookRegistry;
import cn.sgnxotsmicf.app.superagent.interceptor.InterceptorRegistry;
import cn.sgnxotsmicf.common.model.ModelCommon;
import cn.sgnxotsmicf.common.vo.ChatRequest;
import org.redisson.api.RedissonClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.minimax.MiniMaxChatModel;
import org.springframework.ai.minimax.MiniMaxChatOptions;
import org.springframework.stereotype.Component;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/29 15:32
 * @Version: 1.0
 * @Description:
 */
@Component
public class MiniMaxAgentFactory extends AbstractReactAgentCreator {

    private final MiniMaxChatModel miniMaxChatModel;

    public MiniMaxAgentFactory(ToolRegistry toolRegistry, HookRegistry hookRegistry, InterceptorRegistry interceptorRegistry, RedissonClient redissonClient, MiniMaxChatModel miniMaxChatModel) {
        super(toolRegistry, hookRegistry, interceptorRegistry, redissonClient);
        this.miniMaxChatModel = miniMaxChatModel;
    }

    @Override
    public ChatModel createChatModel(ChatRequest request) {
        return miniMaxChatModel;
    }

    @Override
    public ChatOptions createChatOptions(ChatRequest request) {
        String modelId = request.getModelId();
        return MiniMaxChatOptions.builder()
                .model(modelId)
                .maxTokens(request.getMaxTokens().intValue())
                .temperature(request.getTemperature())
                .topP(request.getTopP())
                .build();
    }

    @Override
    public String getSupportedModelIdPrefix() {
        return ModelCommon.MINIMAX_PREFIX;
    }
}
