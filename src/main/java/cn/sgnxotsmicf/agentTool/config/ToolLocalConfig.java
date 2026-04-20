package cn.sgnxotsmicf.agentTool.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 * @Author: lixiang
 * @CreateDate: 2026/3/20 15:21
 * @Version: 1.0
 * @Description:
 */
@Data
@Component
@ConfigurationProperties(prefix = "agent.tool")
public class ToolLocalConfig {

    @Bean
    public String baseDir() {
        return System.getProperty("user.dir") + "/chat-memory";
    }

    @Bean
    public String ragPath(){
        return System.getProperty("user.dir") + "/rag/*知识库.md";
    }

    public static final String FILE_SAVE_DIR = System.getProperty("user.dir") + "/tmp";

    public final static String sandbox_workspace = "";


}
