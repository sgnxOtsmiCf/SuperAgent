package cn.sgnxotsmicf.service.impl;

import cn.dev33.satoken.stp.StpInterface;
import cn.sgnxotsmicf.common.po.Permission;
import cn.sgnxotsmicf.common.po.Role;
import cn.sgnxotsmicf.dao.PermissionMapper;
import cn.sgnxotsmicf.dao.RoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author: lixiang
 * @CreateDate: 2026/3/31 14:47
 * @Version: 1.0
 * @Description:
 */
@Service
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    private final PermissionMapper PermissionMapper;

    private final RoleMapper RoleMapper;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        String Id = loginId.toString();
        return PermissionMapper.selectPermissionByUserId(Long.parseLong(Id))
                .stream().map(Permission::getCode).collect(Collectors.toList());
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        String Id = loginId.toString();
        return RoleMapper.getRoleList(Long.parseLong(Id)).stream().map(Role::getRole).collect(Collectors.toList());
    }
}
