package com.fu.SWP391_BetaFruit.service;

import com.fu.SWP391_BetaFruit.entity.User;

import java.util.List;

public interface UserService {
    List<User> getAllUsers();
    List<User> searchUsers(String keyword);
    User getUserById(Integer userId);
    void toggleUserStatus(Integer userId);
    void assignRolesToUser(Integer userId, List<Long> roleIds);
}
