package cn.sgnxotsmicf.config;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;


@Configuration
public class AiModelConfig {

    @Primary
    @Bean
    public ChatModel primaryChatModel() {
        return dashScopeChatModel;
    }

    @Resource
    public ChatModel dashScopeChatModel;

    @Resource
    public ChatModel openAiChatModel;

    @Resource
    public ChatModel ollamaChatModel;

    @Resource
    public ChatModel zhiPuAiChatModel;

    @Resource
    public ChatModel deepSeekChatModel;

    @Bean
    public ChatOptions dashScopeChatOptions(){
        return DashScopeChatOptions.builder()
                .enableThinking(true)
                .thinkingBudget(500)
                .enableSearch(true)
                .build();
    }
}