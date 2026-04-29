package cn.sgnxotsmicf.common.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/29 9:30
 * @Version: 1.0
 * @Description:
 */

@EqualsAndHashCode(callSuper = true)
@Data
@TableName(value = "superagent_model_provider", autoResultMap = true)
public class ModelProvider extends BaseEntity {

    @TableField("provider_code")
    private String providerCode;

    @TableField("provider_name")
    private String providerName;

    @TableField("provider_type")
    private ProviderEnum providerType;

    @TableField("base_url")
    private String baseUrl;

    @TableField("api_version")
    private String apiVersion;

    @TableField("auth_type")
    private AuthEnum authType;

    @TableField("timeout_ms")
    private BigDecimal timeoutMS;

    @TableField("max_retries")
    private Integer maxRetries;

    @TableField("rate_limit_qps")
    private Long rateLimitQps;

    @TableField("status")
    private Integer status;

    @TableField("priority")
    private Long priority;

    @TableField("config_json")
    private String configJson;

}
