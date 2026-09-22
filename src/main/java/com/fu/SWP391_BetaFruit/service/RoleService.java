package com.fu.SWP391_BetaFruit.service;

import com.fu.SWP391_BetaFruit.dto.request.admin.RoleRequest;
import com.fu.SWP391_BetaFruit.entity.Role;

import java.util.List;

public interface RoleService {
    List<Role> getAllRoles();
    Role getRoleById(Long id);
    void createRole(RoleRequest roleRequest);
    void updateRole(Long id, RoleRequest roleRequest);
    java.util.Map<String, Object> getAdminRolePageData();
}
