package cn.sgnxotsmicf.dao;

import cn.sgnxotsmicf.common.po.Role;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 * 角色表 Mapper 接口
 * </p>
 *
 * @author sgnxotsmicf
 * @since 2025-12-30
 */
public interface RoleMapper extends BaseMapper<Role> {

    List<Role> getRoleList(Long userId);
}
