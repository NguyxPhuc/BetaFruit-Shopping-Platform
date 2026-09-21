package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.entity.Role;
import com.fu.SWP391_BetaFruit.entity.User;
import com.fu.SWP391_BetaFruit.enums.UserStatus;
import com.fu.SWP391_BetaFruit.repository.RoleRepository;
import com.fu.SWP391_BetaFruit.repository.UserRepository;
import com.fu.SWP391_BetaFruit.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public List<User> searchUsers(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return userRepository.findAll();
        }
        return userRepository.searchUsers(keyword.trim());
    }

    @Override
    public User getUserById(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng với ID: " + userId));
    }

    @Override
    @Transactional
    public void toggleUserStatus(Integer userId) {
        User user = getUserById(userId);

        // Bảo vệ tài khoản quản trị viên chính không bị khóa ngoài ý muốn
        if ("khaidq".equalsIgnoreCase(user.getUsername())) {
            throw new IllegalStateException("Không thể khóa tài khoản quản trị viên hệ thống (khaidq)!");
        }

        if (user.getStatus() == UserStatus.ACTIVE) {
            user.setStatus(UserStatus.DEACTIVATED);
        } else {
            user.setStatus(UserStatus.ACTIVE);
        }

        userRepository.save(user);
    }

    @Override
    @Transactional
    public void assignRolesToUser(Integer userId, List<Long> roleIds) {
        User user = getUserById(userId);

        if (roleIds == null || roleIds.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ít nhất 1 vai trò cho người dùng!");
        }

        List<Role> roles = roleRepository.findAllById(roleIds);
        if (roles.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy vai trò hợp lệ nào trong hệ thống!");
        }

        user.setRoles(roles);
        userRepository.save(user);
    }
}
