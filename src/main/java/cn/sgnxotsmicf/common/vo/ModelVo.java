package cn.sgnxotsmicf.common.vo;

import cn.sgnxotsmicf.common.po.BaseEntity;
import cn.sgnxotsmicf.common.po.ModelEnum;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/28 23:00
 * @Version: 1.0
 * @Description:
 */


@Data
public class ModelVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    /**
     * 模型编码: glm4, deepseek-v4-pro等
     */
    private String modelCode;

    /**
     * 模型名称
     */
    private String modelName;

    /**
     * 供应商Id
     */
    private Long providerId;

    /**
     * 供应商名称
     */
    private String providerName;

    /**
     * 模型类型
     *
     * @see ModelEnum 绑定枚举：llm/embedding/image/audio/multimodal
     */
    private ModelEnum modelType;

    /**
     * 能力标签,例如: ["chat","vision","function_calling","json_mode"]
     */
    private String capabilities;

    /**
     * 上下文窗口大小(token)
     */
    private Integer contextWindow;

    /**
     * 最大输出token数
     */
    private Integer maxOutputTokens;

    /**
     * 输入价格/百万tokens
     */
    private BigDecimal inputPricePer1M;

    /**
     * 输出价格/百万tokens
     */
    private BigDecimal outputPricePer1M;

    /**
     * 币种
     */
    private String currency;

    /**
     * 计费单位: token, request, minute
     */
    private String billingUnit;

    /**
     * 参数范围约束
     */
    private String paramConstraints;

    /**
     * 是否推荐 0:否 1:是
     */
    @TableField("is_recommended")
    private Integer isRecommended;

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
