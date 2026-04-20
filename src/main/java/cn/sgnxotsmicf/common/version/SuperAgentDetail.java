package cn.sgnxotsmicf.common.version;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/19 16:35
 * @Version: 1.0
 * @Description: SuperAgent版本详细信息，包含优点和不足
 */

@Data
@Component
public class SuperAgentDetail {

    private SuperAgentVersion version;

    private Map<String, String> advantages;

    private Map<String, String> deficiencies;

}
