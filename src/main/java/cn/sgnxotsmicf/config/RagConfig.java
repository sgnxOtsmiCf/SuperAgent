package cn.sgnxotsmicf.config;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.ai.rag.preretrieval.query.transformation.CompressionQueryTransformer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author: lixiang
 * @CreateDate: 2026/3/23 15:25
 * @Version: 1.0
 * @Description:
 */

@Configuration
public class RagConfig {

    @Resource
    private ChatModel dashScopeChatModel;

    /**
     * 预检索|多查询扩展:使用大语言模型将一个查询扩展为多个语义上的不同变体，有助于检索额外的上下文信息并增加找到相关结果的机会
     * @return MultiQueryExpander bean
     */
    @Bean
    public MultiQueryExpander multiQueryExpander() {
        return MultiQueryExpander.builder()
                .chatClientBuilder(ChatClient.builder(dashScopeChatModel))
                .numberOfQueries(3)
                .build();
    }

    /**
     * 预检索|查询压缩:使用大模型将历史对话和后续查询压缩成一个独立的查询，类似与概括总结，适用于对话历史较长且后续查询与对话上下文相关的场景
     * @return QueryTransformer bean
     */
    @Bean
    public CompressionQueryTransformer compressionQueryTransformer() {
        return CompressionQueryTransformer.builder()
                .chatClientBuilder(ChatClient.builder(dashScopeChatModel))
                .build();
    }



}
