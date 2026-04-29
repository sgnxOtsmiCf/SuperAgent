package cn.sgnxotsmicf.common.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;

/**
 * @Author: lixiang
 * @CreateDate: 2026/3/31 15:16
 * @Version: 1.0
 * @Description:
 */
@EqualsAndHashCode(callSuper = false)
@Data
@TableName("superagent_user")
public class User extends BaseEntity{

    @TableField("username")
    private String username;

    @TableField("password")
    private String password;

    @TableField("nick_name")
    private String nickName;

    @TableField("avatar")
    private String avatar;

    @TableField("phone")
    private String phone;

    @TableField("user_status")
    private Integer userStatus;

    @TableField(exist = false)
    private List<String> permissionList;

    @TableField(exist = false)
    private List<String> roleList;

}
