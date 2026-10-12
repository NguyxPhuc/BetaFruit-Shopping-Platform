package com.fu.SWP391_BetaFruit.service;

import com.fu.SWP391_BetaFruit.dto.request.ChangePasswordRequest;
import com.fu.SWP391_BetaFruit.dto.response.CustomerProfileResponse;
import com.fu.SWP391_BetaFruit.dto.response.ShopProfileResponse;
import com.fu.SWP391_BetaFruit.entity.User;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

public interface UserService {
    CustomerProfileResponse getCustomerProfile(Integer id) throws Exception;
    ShopProfileResponse getShopProfile(Integer id) throws Exception;

    User getUserById(Integer userId);
    void toggleUserStatus(Integer userId);
    void assignRolesToUser(Integer userId, List<Long> roleIds);

    Page<User> getAdminUserPage(String keyword, Long roleId, String status, int page, int size, String sortBy, String sortDir);
    Map<String, Object> getAdminUserPageData(String keyword, Long roleId, String status, int page, int size, String sortBy, String sortDir);

    void changePassword(Integer userId, ChangePasswordRequest changePasswordRequest) throws Exception;
}
