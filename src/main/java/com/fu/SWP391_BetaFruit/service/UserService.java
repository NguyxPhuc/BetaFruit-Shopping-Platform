package com.fu.SWP391_BetaFruit.service;


import com.fu.SWP391_BetaFruit.dto.request.ChangePasswordRequest;
import com.fu.SWP391_BetaFruit.dto.response.CustomerProfileResponse;
import com.fu.SWP391_BetaFruit.dto.response.ShopProfileResponse;
import com.fu.SWP391_BetaFruit.entity.User;
import java.util.Map;
import java.util.List;

public interface UserService {
    CustomerProfileResponse getCustomerProfile(Integer id) throws Exception;
    ShopProfileResponse getShopProfile(Integer id) throws Exception;

    List<User> getAllUsers();
    List<User> searchUsers(String keyword);
    User getUserById(Integer userId);
    void toggleUserStatus(Integer userId);
    void assignRolesToUser(Integer userId, List<Long> roleIds);
    Map<String, Object> getAdminUserPageData(String keyword);

    void changePassword(Integer userId, ChangePasswordRequest changePasswordRequest) throws Exception;
}
