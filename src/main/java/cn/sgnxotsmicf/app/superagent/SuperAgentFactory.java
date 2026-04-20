package cn.sgnxotsmicf.app.superagent;

import cn.sgnxotsmicf.agentTool.ToolRegistry;
import cn.sgnxotsmicf.app.Prompt;
import cn.sgnxotsmicf.app.superagent.hook.HookRegistry;
import cn.sgnxotsmicf.app.superagent.interceptor.InterceptorRegistry;
import cn.sgnxotsmicf.chatMemory.RedissonStore;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.savers.redis.RedisSaver;
import com.alibaba.cloud.ai.graph.store.Store;
import jakarta.annotation.Resource;
import org.redisson.api.RedissonClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.deepseek.DeepSeekChatOptions;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import org.springframework.ai.zhipuai.ZhiPuAiChatModel;
import org.springframework.ai.zhipuai.ZhiPuAiChatOptions;
import org.springframework.ai.zhipuai.api.ZhiPuAiApi;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Map;

@Component
public class SuperAgentFactory {

    private final ToolRegistry toolRegistry;
    private final HookRegistry hookRegistry;
    private final InterceptorRegistry interceptorRegistry;
    private final RedissonClient redissonClient;
    private final ChatModel dashscopeChatModel;
    private final ChatOptions dashscopeChatOptions;
    private final ZhiPuAiChatModel zhiPuAiChatModel;

    @Resource
    private DeepSeekChatModel deepSeekChatModel;

    // 支持的模型常量定义
    public static final String MODEL_QWEN_PLUS = "qwen-plus";
    public static final String MODEL_QWEN_MAX = "qwen-max";
    public static final String MODEL_QWEN_TURBO = "qwen-turbo";
    public static final String MODEL_DEEPSEEK_CHAT = "deepseek-chat";
    public static final String MODEL_DEEPSEEK_REASONER = "deepseek-reasoner";
    public static final String MODEL_GLM_4 = "glm-4";
    public static final String MODEL_GLM_4_PLUS = "glm-4-plus";
    public static final String MODEL_GLM_4_FLASH = "glm-4-flash";
    public static final String MODEL_GLM_4_5_AIRX = "glm-4.5-airx";
    public static final String MODEL_GLM_4_6_V = "glm-4.6v";
    public static final String MODEL_GLM_4_7= "glm-4.7";


    public SuperAgentFactory(
            ToolRegistry toolRegistry,
            HookRegistry hookRegistry,
            InterceptorRegistry interceptorRegistry,
            RedissonClient redissonClient,
            ChatOptions dashscopeChatOptions,
            ChatModel dashscopeChatModel,
            ZhiPuAiChatModel zhiPuAiChatModel) {
        this.toolRegistry = toolRegistry;
        this.hookRegistry = hookRegistry;
        this.interceptorRegistry = interceptorRegistry;
        this.redissonClient = redissonClient;
        this.dashscopeChatOptions = dashscopeChatOptions;
        this.dashscopeChatModel = dashscopeChatModel;
        this.zhiPuAiChatModel = zhiPuAiChatModel;
    }

    /**
     * 创建 Agent，根据 modelId 选择对应的模型
     * @param toolContextConfig 工具上下文配置
     * @param modelId 模型ID，为空时默认使用 qwen-plus
     * @return ReactAgent 实例
     */
    public ReactAgent createAgent(Map<String, Object> toolContextConfig, String modelId) {

        // 根据 modelId 选择对应的模型
        ChatModel selectedModel = selectModel(modelId);

        // 根据模型类型选择对应的 ChatOptions
        ChatOptions selectedOptions = selectChatOptions(modelId);

        return ReactAgent.builder()
                .name("SuperAgent")
                .model(selectedModel)
                .tools(toolRegistry.SuperAgentTool())
                .toolContext(toolContextConfig)
                .hooks(hookRegistry.buildHooks())
                .interceptors(interceptorRegistry.buildInterceptors())
                .saver(buildRedisSaver())
                .systemPrompt(Prompt.SuperAgentSystemPrompt)
                .enableLogging(true)
                .chatOptions(selectedOptions)
                .build();
    }


    /**
     * 根据 modelId 选择对应的 ChatModel
     */
    private ChatModel selectModel(String modelId) {
        // 默认使用 dashscope (qwen)
        if (modelId == null || modelId.isEmpty()) {
            return dashscopeChatModel;
        }

        return switch (modelId.toLowerCase()) {
            case MODEL_DEEPSEEK_CHAT, MODEL_DEEPSEEK_REASONER -> deepSeekChatModel;
            case MODEL_GLM_4, MODEL_GLM_4_PLUS, MODEL_GLM_4_FLASH, MODEL_GLM_4_5_AIRX, MODEL_GLM_4_6_V,MODEL_GLM_4_7 -> zhiPuAiChatModel;
            case MODEL_QWEN_PLUS, MODEL_QWEN_MAX, MODEL_QWEN_TURBO -> dashscopeChatModel;
            default -> dashscopeChatModel; // 默认使用 qwen
        };
    }

    /**
     * 根据 modelId 选择对应的 ChatOptions
     */
    private ChatOptions selectChatOptions(String modelId) {
        // 默认使用 dashscope 的 options
        if (modelId == null || modelId.isEmpty()) {
            return dashscopeChatOptions;
        }

        String lowerModelId = modelId.toLowerCase();

        // 千问系列使用 DashScopeChatOptions
        if (lowerModelId.startsWith("qwen")) {
            return DashScopeChatOptions.builder()
                    .model(lowerModelId)
                    .enableThinking(true)
                    .enableSearch(true)
                    .thinkingBudget(1000)
                    .maxToken(10000)
                    .build();
        }else if (lowerModelId.startsWith("glm")) {
            return ZhiPuAiChatOptions.builder()
                    .model(lowerModelId)
                    .maxTokens(10000)
                    .thinking(ZhiPuAiApi.ChatCompletionRequest.Thinking.enabled())
                    .build();
        }else if (lowerModelId.startsWith("deepseek")) {
            return DeepSeekChatOptions.builder()
                    .model(lowerModelId)
                    .maxTokens(10000)
                    .build();
        }
        // 默认使用 dashscopeChatOptions
        return dashscopeChatOptions;
    }

    /**
     * 创建 Agent（兼容旧版本，默认使用 qwen-plus）
     */
    public ReactAgent createAgent(Map<String, Object> toolContextConfig) {
        return createAgent(toolContextConfig, null);
    }



    public RedisSaver buildRedisSaver() {
        return RedisSaver.builder()
                .redisson(redissonClient)
                .build();
    }

    public Store buildRedisStore() {
        return new RedissonStore(redissonClient, "spring-ai-graph:store:");
    }



}