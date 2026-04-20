package cn.sgnxotsmicf.agentTool.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Tavily 搜索配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "tavily")
public class TavilySearchProperties {
    /**
     * Tavily API密钥
     */
    private String apiKey;

    /**
     * Tavily API地址
     */
    private String apiUrl;

    /**
     * 最大重试次数
     */
    private Integer maxRetries = 2;
}