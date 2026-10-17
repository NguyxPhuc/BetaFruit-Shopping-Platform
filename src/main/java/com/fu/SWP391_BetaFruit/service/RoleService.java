package com.fu.SWP391_BetaFruit.service;

import com.fu.SWP391_BetaFruit.dto.request.RoleRequest;
import com.fu.SWP391_BetaFruit.entity.Role;

import java.util.List;
import java.util.Map;

public interface RoleService {
    List<Role> getAllRoles();
    Role getRoleById(Long id);
    void createRole(RoleRequest roleRequest);
    void updateRole(Long id, RoleRequest roleRequest);
    void toggleRoleStatus(Long id);
    Map<String, Object> getAdminRolePageData();
}
