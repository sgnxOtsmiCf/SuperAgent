package cn.sgnxotsmicf.common.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/29 10:05
 * @Version: 1.0
 * @Description:
 */

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("superagent_model_group")
public class ModelGroup extends BaseEntity{

    @TableField("group_name")
    private String groupName;

    @TableField("group_code")
    private String groupCode;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("status")
    private Integer status;

}
