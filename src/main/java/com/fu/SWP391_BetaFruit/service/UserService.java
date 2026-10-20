package com.fu.SWP391_BetaFruit.service;

import com.fu.SWP391_BetaFruit.dto.request.ChangePasswordRequest;
import com.fu.SWP391_BetaFruit.dto.request.CustomerProfileRequest;
import com.fu.SWP391_BetaFruit.dto.request.ShopProfileRequest;
import com.fu.SWP391_BetaFruit.dto.request.UserRequest;
import com.fu.SWP391_BetaFruit.dto.response.CustomerProfileResponse;
import com.fu.SWP391_BetaFruit.dto.response.ShopProfileResponse;
import com.fu.SWP391_BetaFruit.entity.User;

import java.util.List;
import java.util.Map;

public interface UserService {
    CustomerProfileResponse getCustomerProfile(Integer id) throws Exception;

    User getUserById(Integer userId);
    void toggleUserStatus(Integer userId);
    void assignRolesToUser(Integer userId, List<Long> roleIds);
    void createUser(UserRequest userRequest);

    long countUsers();
    Map<String, Object> getAdminUserPageData(String keyword, Long roleId, String status, int page, int size, String sortBy, String sortDir);

    void changePassword(Integer userId, ChangePasswordRequest changePasswordRequest) throws Exception;

    void editCustomerProfile(Integer userId, CustomerProfileRequest customerProfileRequest) throws Exception;
}
