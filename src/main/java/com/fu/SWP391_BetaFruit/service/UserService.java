package com.fu.SWP391_BetaFruit.service;

import com.fu.SWP391_BetaFruit.entity.User;

import java.util.List;

public interface UserService {
    List<User> getAllUsers();
    List<User> searchUsers(String keyword);
    User getUserById(Integer userId);
    void toggleUserStatus(Integer userId);
    void assignRolesToUser(Integer userId, List<Long> roleIds);
    org.springframework.data.domain.Page<User> getAdminUserPage(String keyword, int page, int size);
    org.springframework.data.domain.Page<User> getAdminUserPage(String keyword, int page, int size, String sortBy, String sortDir);
    org.springframework.data.domain.Page<User> getAdminUserPage(String keyword, Long roleId, String status, int page, int size, String sortBy, String sortDir);
    java.util.Map<String, Object> getAdminUserPageData(String keyword);
    java.util.Map<String, Object> getAdminUserPageData(String keyword, int page, int size);
    java.util.Map<String, Object> getAdminUserPageData(String keyword, int page, int size, String sortBy, String sortDir);
    java.util.Map<String, Object> getAdminUserPageData(String keyword, Long roleId, String status, int page, int size, String sortBy, String sortDir);
}
