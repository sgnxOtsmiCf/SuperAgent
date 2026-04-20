package cn.sgnxotsmicf.config;

import com.knuddels.jtokkit.api.EncodingType;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author: lixiang
 * @CreateDate: 2026/3/23 15:14
 * @Version: 1.0
 * @Description:
 */
@Configuration
public class EmbeddingConfig {

    @Bean
    public BatchingStrategy customBatchingStrategy() {
        return new TokenCountBatchingStrategy(
                EncodingType.CL100K_BASE,//指定编码类型
                8000,//设置最大输入token数
                0.1 //设置保留百分比
        );
    }
}
