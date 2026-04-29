package cn.sgnxotsmicf.app.superagent.factory;

import cn.sgnxotsmicf.agentTool.ToolRegistry;
import cn.sgnxotsmicf.app.superagent.hook.HookRegistry;
import cn.sgnxotsmicf.app.superagent.interceptor.InterceptorRegistry;
import cn.sgnxotsmicf.common.model.ModelCommon;
import cn.sgnxotsmicf.common.vo.ChatRequest;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import org.redisson.api.RedissonClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.stereotype.Component;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/27 17:19
 * @Version: 0.1
 * @Description:
 */

@Component
public class QwenAgentCreator extends AbstractReactAgentCreator{

    private final ChatModel dashscopeChatModel;

    public QwenAgentCreator(ToolRegistry toolRegistry,
                            HookRegistry hookRegistry,
                            InterceptorRegistry interceptorRegistry,
                            RedissonClient redissonClient,
                            ChatModel dashscopeChatModel) {
        super(toolRegistry, hookRegistry, interceptorRegistry, redissonClient);
        this.dashscopeChatModel = dashscopeChatModel;
    }

    @Override
    public ChatModel createChatModel(ChatRequest request) {
        //先暂时这样设置
        return dashscopeChatModel;
    }

    @Override
    public ChatOptions createChatOptions(ChatRequest request) {
        String modelId = request.getModelId();
        String lowerModelId = modelId != null ? modelId.toLowerCase() : "qwen-plus";
        return DashScopeChatOptions.builder()
                .model(lowerModelId)
                .enableThinking(request.getEnableThinking())
                .enableSearch(request.getEnableSearch())
                .thinkingBudget(request.getThinkingBudget().intValue())
                .maxToken(request.getMaxTokens().intValue())
                .temperature(request.getTemperature())
                .topP(request.getTopP())
                .topK(request.getTopK().intValue())
                .build();
    }

    @Override
    public String getSupportedModelIdPrefix() {
        return ModelCommon.QWEN_PREFIX;
    }
}
