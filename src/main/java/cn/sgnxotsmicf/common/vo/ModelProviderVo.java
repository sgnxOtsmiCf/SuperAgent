package cn.sgnxotsmicf.common.vo;

import cn.sgnxotsmicf.common.po.AuthEnum;
import cn.sgnxotsmicf.common.po.ProviderEnum;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/29 9:30
 * @Version: 1.0
 * @Description:
 */

@Data
public class ModelProviderVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String providerCode;

    private String providerName;

    private ProviderEnum providerType;

    private String baseUrl;

    private String apiVersion;

    private AuthEnum authType;

    private BigDecimal timeoutMS;

    private Integer maxRetries;

    private Long rateLimitQps;

    private String configJson;

    private List<ModelVo> modelVoList;
}
