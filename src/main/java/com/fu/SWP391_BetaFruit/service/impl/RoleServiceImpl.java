package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.dto.admin.RoleDto;
import com.fu.SWP391_BetaFruit.entity.Role;
import com.fu.SWP391_BetaFruit.repository.RoleRepository;
import com.fu.SWP391_BetaFruit.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
    public void createRole(RoleDto roleDto) {
        if (roleDto.getRoleName() == null || roleDto.getRoleName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên vai trò không được để trống!");
        }
        String cleanRoleName = roleDto.getRoleName().trim();
        if (roleRepository.existsByRoleName(cleanRoleName)) {
            throw new IllegalArgumentException("Tên vai trò '" + cleanRoleName + "' đã tồn tại trong hệ thống!");
        }
        Role role = new Role();
        role.setRoleName(cleanRoleName);
        roleRepository.save(role);
    }

    @Override
    @Transactional
    public void updateRole(Long id, RoleDto roleDto) {
        Role role = getRoleById(id);
        if (roleDto.getRoleName() == null || roleDto.getRoleName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên vai trò không được để trống!");
        }
        String cleanRoleName = roleDto.getRoleName().trim();
        if (!role.getRoleName().equalsIgnoreCase(cleanRoleName) && roleRepository.existsByRoleName(cleanRoleName)) {
            throw new IllegalArgumentException("Tên vai trò '" + cleanRoleName + "' đã tồn tại!");
        }
        role.setRoleName(cleanRoleName);
        roleRepository.save(role);
    }
}
