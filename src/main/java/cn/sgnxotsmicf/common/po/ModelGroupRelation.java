package cn.sgnxotsmicf.common.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/29
 * @Version: 1.0
 * @Description: 模型分组关联表
 */

@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@TableName("superagent_model_group_relation")
public class ModelGroupRelation extends BaseEntity {

    @TableField("model_id")
    private Long modelId;

    @TableField("group_id")
    private Long groupId;

}
