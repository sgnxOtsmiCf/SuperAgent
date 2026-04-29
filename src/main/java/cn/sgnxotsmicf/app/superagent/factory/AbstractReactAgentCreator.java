package cn.sgnxotsmicf.app.superagent.factory;

import cn.sgnxotsmicf.agentTool.ToolRegistry;
import cn.sgnxotsmicf.app.superagent.hook.HookRegistry;
import cn.sgnxotsmicf.app.superagent.interceptor.InterceptorRegistry;
import cn.sgnxotsmicf.common.agent.Prompt;
import cn.sgnxotsmicf.common.vo.ChatRequest;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.savers.redis.RedisSaver;
import org.redisson.api.RedissonClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;

import java.util.Map;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/27 16:33
 * @Version: 0.1
 * @Description:
 */

public abstract class AbstractReactAgentCreator {

    protected final ToolRegistry toolRegistry;
    protected final HookRegistry hookRegistry;
    protected final InterceptorRegistry interceptorRegistry;
    protected final RedissonClient redissonClient;

    public AbstractReactAgentCreator (
            ToolRegistry toolRegistry,
            HookRegistry hookRegistry,
            InterceptorRegistry interceptorRegistry,
            RedissonClient redissonClient) {
        this.toolRegistry = toolRegistry;
        this.hookRegistry = hookRegistry;
        this.interceptorRegistry = interceptorRegistry;
        this.redissonClient = redissonClient;
    }

    /**
     * 创建 ChatModel
     */
    public abstract ChatModel createChatModel(ChatRequest request);

    /**
     * 创建 ChatOptions
     * @param request 请求参数
     */
    public abstract ChatOptions createChatOptions(ChatRequest request);


    /**
     * 策略标识：当前创造器支持哪个 modelId
     */
    public abstract String getSupportedModelIdPrefix();

    /**
     * 模板方法：创建 Agent
     */
    public final ReactAgent createAgent(Map<String, Object> toolContextConfig,  ChatRequest request) {
        String modelId = request.getModelId();

        ChatModel selectedModel = createChatModel(request);
        ChatOptions selectedOptions = createChatOptions(request);

        return ReactAgent.builder()
                .name(getAgentName())
                .model(selectedModel)
                .tools(toolRegistry.SuperAgentTool())
                .toolContext(toolContextConfig)
                .hooks(hookRegistry.buildHooks())
                .interceptors(interceptorRegistry.buildInterceptors())
                .saver(buildRedisSaver())
                .systemPrompt(getSystemPrompt())
                .enableLogging(isLoggingEnabled())
                .chatOptions(selectedOptions)
                .build();
    }


    protected String getAgentName() {
        return "SuperAgent";
    }


    protected String getSystemPrompt() {
        return Prompt.SuperAgentSystemPrompt;
    }


    protected boolean isLoggingEnabled() {
        return true;
    }


    public RedisSaver buildRedisSaver() {
        return RedisSaver.builder()
                .redisson(redissonClient)
                .build();
    }

}
