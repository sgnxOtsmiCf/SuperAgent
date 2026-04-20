package cn.sgnxotsmicf.rag;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentRetriever;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentRetrieverOptions;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
/**
 * @Author: lixiang
 * @CreateDate: 2026/3/22 15:18
 * @Version: 1.0
 * @Description:
 */

@Configuration
@ConfigurationProperties(prefix = "spring.ai.dashscope")
@Slf4j
public class RagCloudAdvisorConfig {


    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;

    @Resource
    private ChatModel dashScopeChatModel;

    @Bean(name = "ragCloudAdvisor")
    public Advisor ragCloudAdvisor() {
        DashScopeApi dashScopeApi = new DashScopeApi.Builder().apiKey(apiKey).build();
        DashScopeDocumentRetriever dashScopeDocumentRetriever = new DashScopeDocumentRetriever(
                dashScopeApi,
                DashScopeDocumentRetrieverOptions.builder()
                        .indexName("家庭和睦知识库")
                        .build());

        return RetrievalAugmentationAdvisor.builder()
                .queryAugmenter(ContextualQueryAugmenter.builder().allowEmptyContext(true).build()) //空上下文处理
                .documentRetriever(dashScopeDocumentRetriever)
                .build();
    }

}
