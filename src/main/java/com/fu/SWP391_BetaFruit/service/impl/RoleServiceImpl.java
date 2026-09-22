package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.dto.request.admin.RoleRequest;
import com.fu.SWP391_BetaFruit.entity.Role;
import com.fu.SWP391_BetaFruit.repository.RoleRepository;
import com.fu.SWP391_BetaFruit.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Role getRoleById(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy vai trò với ID: " + id));
    }

    @Override
    @Transactional
    public void createRole(RoleRequest roleRequest) {
        if (roleRequest.getRoleName() == null || roleRequest.getRoleName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên vai trò không được để trống!");
        }
        String cleanRoleName = roleRequest.getRoleName().trim();
        if (roleRepository.existsByRoleName(cleanRoleName)) {
            throw new IllegalArgumentException("Tên vai trò '" + cleanRoleName + "' đã tồn tại trong hệ thống!");
        }
        Role role = new Role();
        role.setRoleName(cleanRoleName);
        roleRepository.save(role);
    }

    @Override
    @Transactional
    public void updateRole(Long id, RoleRequest roleRequest) {
        Role role = getRoleById(id);
        if (roleRequest.getRoleName() == null || roleRequest.getRoleName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên vai trò không được để trống!");
        }
        String cleanRoleName = roleRequest.getRoleName().trim();
        if (!role.getRoleName().equalsIgnoreCase(cleanRoleName) && roleRepository.existsByRoleName(cleanRoleName)) {
            throw new IllegalArgumentException("Tên vai trò '" + cleanRoleName + "' đã tồn tại!");
        }
        role.setRoleName(cleanRoleName);
        roleRepository.save(role);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getAdminRolePageData() {
        Map<String, Object> data = new HashMap<>();
        data.put("roles", getAllRoles());
        data.put("newRole", new RoleRequest());
        return data;
    }
}
