package cn.sgnxotsmicf.config;

import io.minio.MinioClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/7 13:55
 * @Version: 1.0
 * @Description:
 */
@Configuration
@Data
@ConfigurationProperties(prefix = "spring.ai.alibaba.toolcalling.minio")
public class MinioConfig {

    private String endpoint;

    private String accessKey;

    private String secretKey;

    /**
     * 注入 MinioClient 客户端
     */
    @Bean
    public MinioClient superAgentMinioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}
