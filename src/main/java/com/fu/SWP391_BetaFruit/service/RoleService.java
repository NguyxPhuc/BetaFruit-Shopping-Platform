package com.fu.SWP391_BetaFruit.service;

import com.fu.SWP391_BetaFruit.dto.admin.RoleDto;
import com.fu.SWP391_BetaFruit.entity.Role;

import java.util.List;

public interface RoleService {
    List<Role> getAllRoles();
    Role getRoleById(Long id);
    void createRole(RoleDto roleDto);
    void updateRole(Long id, RoleDto roleDto);
}
