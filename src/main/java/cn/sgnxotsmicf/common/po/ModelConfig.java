package cn.sgnxotsmicf.common.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/29 9:10
 * @Version: 1.0
 * @Description:
 */

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("superagent_user_model_config")
public class ModelConfig extends BaseEntity{

    @TableField("user_id")
    private Long userId;

    @TableField("temperature")
    private BigDecimal temperature;

    @TableField("top_p")
    private BigDecimal topP;

    @TableField("top_k")
    private BigDecimal topK;

    @TableField("max_tokens")
    private Long maxTokens;


}
