package cn.sgnxotsmicf.agentTool.config;

import cn.sgnxotsmicf.agentTool.onlinetool.SmartWebFetchTool;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/9 17:30
 * @Version: 1.0
 * @Description:
 */

@Configuration
public class ToolOnlineConfig {


    @Resource
    private ChatModel dashscopeChatModel;

    /**
     * 注册 SmartWebFetchTool 工具Bean
     * Spring AI 自动识别 @Tool 注解，实现AI函数调用
     */
    @Bean
    public SmartWebFetchTool smartWebFetchTool() {
        ChatClient chatClient = ChatClient.builder(dashscopeChatModel).build();
        return SmartWebFetchTool.builder(chatClient)
                // 关闭域名安全检查（国内无法访问Claude API，必关！）
                .domainSafetyCheck(false)
                // 最大内容长度
                .maxContentLength(100000)
                // 缓存大小
                .maxCacheSize(100)
                // 最大重试次数
                .maxRetries(2)
                .build();
    }
}
