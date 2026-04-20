package cn.sgnxotsmicf.dao;

import cn.sgnxotsmicf.common.po.Permission;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 权限表 Mapper 接口
 * </p>
 *
 * @author sgnxotsmicf
 * @since 2026-01-04
 */
public interface PermissionMapper extends BaseMapper<Permission> {

    List<Permission> selectPermissionByUserId(@Param("userId") Long userId);
}
