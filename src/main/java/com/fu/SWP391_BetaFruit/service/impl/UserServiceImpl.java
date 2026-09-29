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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    private static final Set<String> ALLOWED_USER_SORT_FIELDS = Set.of(
            "userId", "username", "fullName", "email", "status", "createdAt"
    );

    @Override
    public Page<User> getAdminUserPage(String keyword, int page, int size) {
        return getAdminUserPage(keyword, null, null, page, size, "userId", "desc");
    }

    @Override
    public Page<User> getAdminUserPage(String keyword, int page, int size, String sortBy, String sortDir) {
        return getAdminUserPage(keyword, null, null, page, size, sortBy, sortDir);
    }

    @Override
    public Page<User> getAdminUserPage(String keyword, Long roleId, String status, int page, int size, String sortBy, String sortDir) {
        int pageIndex = Math.max(0, page - 1);
        int pageSize = size > 0 ? size : 5;

        String safeSortBy = (sortBy != null && ALLOWED_USER_SORT_FIELDS.contains(sortBy.trim()))
                ? sortBy.trim() : "userId";
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(pageIndex, pageSize, Sort.by(direction, safeSortBy));

        String cleanKeyword = (keyword != null) ? keyword.trim() : "";
        UserStatus filterStatus = parseUserStatus(status);
        Long validRoleId = (roleId != null && roleId > 0) ? roleId : null;

        boolean hasKeyword = !cleanKeyword.isEmpty();
        boolean hasRole = (validRoleId != null);
        boolean hasStatus = (filterStatus != null);

        if (!hasKeyword && !hasRole && !hasStatus) {
            return userRepository.findAll(pageable);
        }

        return userRepository.filterUsers(
                hasKeyword ? cleanKeyword : null,
                validRoleId,
                filterStatus,
                pageable
        );
    }

    private UserStatus parseUserStatus(String statusStr) {
        if (statusStr == null || statusStr.trim().isEmpty() || "ALL".equalsIgnoreCase(statusStr.trim())) {
            return null;
        }
        String clean = statusStr.trim();
        try {
            return UserStatus.valueOf(clean.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            if ("active".equalsIgnoreCase(clean)) {
                return UserStatus.ACTIVE;
            }
            if ("deactivated".equalsIgnoreCase(clean) || "locked".equalsIgnoreCase(clean)) {
                return UserStatus.DEACTIVATED;
            }
        }
        return null;
    }

    @Override
    public Map<String, Object> getAdminUserPageData(String keyword) {
        return getAdminUserPageData(keyword, null, null, 1, 5, "userId", "desc");
    }

    @Override
    public Map<String, Object> getAdminUserPageData(String keyword, int page, int size) {
        return getAdminUserPageData(keyword, null, null, page, size, "userId", "desc");
    }

    @Override
    public Map<String, Object> getAdminUserPageData(String keyword, int page, int size, String sortBy, String sortDir) {
        return getAdminUserPageData(keyword, null, null, page, size, sortBy, sortDir);
    }

    @Override
    public Map<String, Object> getAdminUserPageData(String keyword, Long roleId, String status, int page, int size, String sortBy, String sortDir) {
        String cleanKeyword = (keyword != null) ? keyword.trim() : "";
        String safeSortBy = (sortBy != null && ALLOWED_USER_SORT_FIELDS.contains(sortBy.trim()))
                ? sortBy.trim() : "userId";
        String safeSortDir = "asc".equalsIgnoreCase(sortDir) ? "asc" : "desc";
        Long validRoleId = (roleId != null && roleId > 0) ? roleId : null;
        UserStatus filterStatus = parseUserStatus(status);

        Page<User> userPage = getAdminUserPage(cleanKeyword, validRoleId, status, page, size, safeSortBy, safeSortDir);
        List<Role> allRoles = roleRepository.findAll();

        Map<String, Object> data = new HashMap<>();
        data.put("users", userPage.getContent());
        data.put("userPage", userPage);
        data.put("pageData", userPage);
        data.put("allRoles", allRoles);
        data.put("keyword", cleanKeyword);
        data.put("currentRoleId", validRoleId);
        data.put("currentStatus", filterStatus != null ? filterStatus.name() : "ALL");
        data.put("sortBy", safeSortBy);
        data.put("sortDir", safeSortDir);
        data.put("totalCount", userRepository.count());
        data.put("activeCount", userRepository.countByStatus(UserStatus.ACTIVE));
        data.put("lockedCount", userRepository.countByStatus(UserStatus.DEACTIVATED));

        return data;
    }
}
