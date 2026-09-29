package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.dto.response.CustomerProfileResponse;
import com.fu.SWP391_BetaFruit.dto.response.ShopProfileResponse;
import com.fu.SWP391_BetaFruit.entity.CustomerMembership;
import com.fu.SWP391_BetaFruit.entity.Role;
import com.fu.SWP391_BetaFruit.entity.Shop;
import com.fu.SWP391_BetaFruit.entity.User;
import com.fu.SWP391_BetaFruit.enums.UserStatus;
import com.fu.SWP391_BetaFruit.repository.CustomerMembershipRepository;
import com.fu.SWP391_BetaFruit.repository.RoleRepository;
import com.fu.SWP391_BetaFruit.repository.ShopRepository;
import com.fu.SWP391_BetaFruit.repository.UserRepository;
import com.fu.SWP391_BetaFruit.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private CustomerMembershipRepository customerMembershipRepository;

    @Override
    public CustomerProfileResponse getCustomerProfile(Integer id) throws Exception {
        CustomerProfileResponse customerProfileResponse = new CustomerProfileResponse();
        User user = userRepository.findById(id)
                .orElseThrow(() -> new Exception("Lỗi hệ thống: không tìm thấy thông tin người dùng."));
        customerProfileResponse.setEmail(user.getEmail());
        customerProfileResponse.setPhone(user.getPhone());
        customerProfileResponse.setUsername(user.getUsername());
        customerProfileResponse.setFullName(user.getFullName());

        CustomerMembership customerMembership = customerMembershipRepository.findByUserId(user.getUserId())
                .orElseThrow(() -> new Exception("Lỗi hệ thống: người dùng hiện tại chưa có hạng."));
        customerProfileResponse.setTotalSpent(customerMembership.getTotalCumulativeSpend());
        if(customerMembership.getTier() != null){
            customerProfileResponse.setTierName(customerMembership.getTier().getTierName());
        }

        return customerProfileResponse;
    }

    @Override
    public ShopProfileResponse getShopProfile(Integer id) throws Exception {
        ShopProfileResponse shopProfileResponse = new ShopProfileResponse();
        User user = userRepository.findById(id)
                .orElseThrow(() -> new Exception("Lỗi hệ thống: không tìm thấy thông tin người dùng."));

        shopProfileResponse.setEmail(user.getEmail());
        shopProfileResponse.setPhone(user.getPhone());

        Shop shop = shopRepository.findByOwner(user)
                .orElseThrow(() -> new Exception("Lỗi hệ thống: không tìm thấy thông tin shop."));

        shopProfileResponse.setShopName(shop.getShopName());
        shopProfileResponse.setShopDescription(shop.getShopDescription());
        shopProfileResponse.setBankName(shop.getBankName());
        shopProfileResponse.setBankAccountNumber(shop.getBankAccountNumber());
        shopProfileResponse.setAccountName(shop.getAccountName());


        return shopProfileResponse;
    }
  
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

    @Override
    public Map<String, Object> getAdminUserPageData(String keyword) {
        String cleanKeyword = (keyword != null) ? keyword.trim() : "";
        List<User> users = searchUsers(cleanKeyword);
        List<Role> allRoles = roleRepository.findAll();

        Map<String, Object> data = new HashMap<>();
        data.put("users", users);
        data.put("allRoles", allRoles);
        data.put("keyword", cleanKeyword);

        return data;
    }
}
