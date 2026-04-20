package cn.sgnxotsmicf.common.po;

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
@TableName("superagent_permission")
public class Permission extends BaseEntity{

    /**
     * 权限名称
     */
    private String name;

    /**
     * 权限代码(权限标识符)
     */
    private String code;

    /**
     * URL
     */
    private String url;

    /**
     * 权限类型: menu-菜单, button-按钮
     */
    private String type;

    /**
     * 父权限id
     */
    private Long parentId;

    /**
     * 菜单排序号: 菜单权限需要排序
     */
    private Integer orderNo;

    /**
     * 菜单图标
     */
    private String icon;

    /**
     * 菜单对应要渲染的Vue组件名称
     */
    private String component;


}
