package cn.sgnxotsmicf.common.version;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/18 13:49
 * @Version: 1.0
 * @Description:
 */

@Component
@Data
@ConfigurationProperties(prefix = "super-agent")
public class SuperAgentVersion {

    private String author;

    private String version;

    private String description;
}
