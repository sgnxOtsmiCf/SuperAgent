package cn.sgnxotsmicf.common.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: lixiang
 * @CreateDate: 2026/3/31 19:48
 * @Version: 1.0
 * @Description:
 */

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("superagent_role")
public class Role extends BaseEntity{

    /**
     * 角色
     */
    @TableField("role")
    private String role;

    /**
     * 角色名称
     */
    @TableField("role_name")
    private String roleName;
}
