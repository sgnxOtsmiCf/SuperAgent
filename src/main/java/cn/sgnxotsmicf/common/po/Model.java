package cn.sgnxotsmicf.common.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/28 23:00
 * @Version: 1.0
 * @Description:
 */

@EqualsAndHashCode(callSuper = true)
@Data
@TableName(value = "superagent_model", autoResultMap = true)
public class Model extends BaseEntity {


    /**
     * 模型编码: glm4, deepseek-v4-pro等
     */
    @TableField("model_code")
    private String modelCode;

    /**
     * 模型名称
     */
    @TableField("model_name")
    private String modelName;

    /**
     * 供应商Id
     */
    @TableField("provider_id")
    private Long providerId;

    /**
     * 供应商名称
     */
    @TableField("provider_name")
    private String providerName;

    /**
     * 模型类型
     *
     * @see ModelEnum 绑定枚举：llm/embedding/image/audio/multimodal
     */
    @TableField("model_type")
    private ModelEnum modelType;

    /**
     * 能力标签,例如: ["chat","vision","function_calling","json_mode"]
     */
    @TableField("capabilities")
    private String capabilities;

    /**
     * 上下文窗口大小(token)
     */
    @TableField("context_window")
    private Integer contextWindow;

    /**
     * 最大输出token数
     */
    @TableField("max_output_tokens")
    private Integer maxOutputTokens;

    /**
     * 输入价格/百万tokens
     */
    @TableField("input_price_per_1m")
    private BigDecimal inputPricePer1M;

    /**
     * 输出价格/百万tokens
     */
    @TableField("output_price_per_1m")
    private BigDecimal outputPricePer1M;

    /**
     * 币种
     */
    @TableField("currency")
    private String currency;

    /**
     * 计费单位: token, request, minute
     */
    @TableField("billing_unit")
    private String billingUnit;

    /**
     * 参数范围约束
     */
    @TableField("param_constraints")
    private String paramConstraints;

    /**
     * 状态 0:下线 1:上线 2:内测
     */
    @TableField("status")
    private Integer status;

    /**
     * 是否推荐 0:否 1:是
     */
    @TableField("is_recommended")
    private Integer isRecommended;

    /**
     * 排序
     */
    @TableField("sort_order")
    private Integer sortOrder;

    /**
     * 模型描述
     */
    @TableField("description")
    private String description;

    /**
     * 图标URL
     */
    @TableField("icon_url")
    private String iconUrl;

    /**
     * 标签: ["new","hot"]
     */
    @TableField("tags")
    private String tags;


}
