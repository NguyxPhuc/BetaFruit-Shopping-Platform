package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.dto.request.ChangePasswordRequest;
import com.fu.SWP391_BetaFruit.dto.request.CustomerProfileRequest;
import com.fu.SWP391_BetaFruit.dto.request.UserRequest;
import com.fu.SWP391_BetaFruit.dto.response.CustomerProfileResponse;
import com.fu.SWP391_BetaFruit.entity.Cart;
import com.fu.SWP391_BetaFruit.entity.CustomerMembership;
import com.fu.SWP391_BetaFruit.entity.MembershipTier;
import com.fu.SWP391_BetaFruit.entity.Role;
import com.fu.SWP391_BetaFruit.entity.User;
import com.fu.SWP391_BetaFruit.enums.RoleName;
import com.fu.SWP391_BetaFruit.enums.UserStatus;
import com.fu.SWP391_BetaFruit.repository.CartRepository;
import com.fu.SWP391_BetaFruit.repository.CustomerMembershipRepository;
import com.fu.SWP391_BetaFruit.repository.MembershipTierRepository;
import com.fu.SWP391_BetaFruit.repository.RoleRepository;
import com.fu.SWP391_BetaFruit.repository.ShopRepository;
import com.fu.SWP391_BetaFruit.repository.UserRepository;
import com.fu.SWP391_BetaFruit.service.UserService;
import lombok.RequiredArgsConstructor;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
    private RoleRepository roleRepository;

    @Autowired
    private CustomerMembershipRepository customerMembershipRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private MembershipTierRepository tierRepository;

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
    public User getUserById(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng với ID: " + userId));
    }

    @Override
    @Transactional
    public void toggleUserStatus(Integer userId) {
        User user = getUserById(userId);

        if (user.getStatus() == UserStatus.ACTIVE) {
            boolean isAdmin = user.getRoles().stream()
                    .anyMatch(r -> r.getRoleName() == RoleName.ADMIN);
            if (isAdmin) {
                long activeAdminCount = userRepository.findAll().stream()
                        .filter(u -> u.getStatus() == UserStatus.ACTIVE && u.getRoles().stream().anyMatch(r -> r.getRoleName() == RoleName.ADMIN))
                        .count();
                if (activeAdminCount <= 1) {
                    throw new IllegalArgumentException("Không thể khóa tài khoản Quản trị viên (ADMIN) duy nhất đang hoạt động trong hệ thống!");
                }
            }
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

        boolean hasInactiveRole = roles.stream()
                .anyMatch(r -> !r.isActive());
        if (hasInactiveRole) {
            Role inactiveRole = roles.stream().filter(r -> !r.isActive()).findFirst().orElse(null);
            String name = inactiveRole != null ? inactiveRole.getRoleName().name() : "";
            throw new IllegalArgumentException("Vai trò [" + name + "] đang ở trạng thái ẩn hoặc tạm ngưng hoạt động, không thể cấp quyền!");
        }

        // Chặn tước quyền Quản trị viên của tài khoản Admin duy nhất đang hoạt động
        boolean wasAdmin = user.getRoles().stream().anyMatch(r -> r.getRoleName() == RoleName.ADMIN);
        boolean willBeAdmin = roles.stream().anyMatch(r -> r.getRoleName() == RoleName.ADMIN);
        if (wasAdmin && !willBeAdmin && user.getStatus() == UserStatus.ACTIVE) {
            long activeAdminCount = userRepository.findAll().stream()
                    .filter(u -> u.getStatus() == UserStatus.ACTIVE && u.getRoles().stream().anyMatch(r -> r.getRoleName() == RoleName.ADMIN))
                    .count();
            if (activeAdminCount <= 1) {
                throw new IllegalArgumentException("Không thể gỡ vai trò Quản trị viên của tài khoản ADMIN duy nhất đang hoạt động!");
            }
        }

        user.setRoles(roles);
        userRepository.save(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createUser(UserRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Dữ liệu người dùng không hợp lệ!");
        }

        String cleanUsername = request.getUsername() != null ? request.getUsername().trim() : "";
        if (cleanUsername.isEmpty()) {
            throw new IllegalArgumentException("Tên đăng nhập không được để trống!");
        }
        if (!cleanUsername.matches("^(?!.*(.)\\1{2,})[a-zA-Z0-9_]{3,50}$")) {
            throw new IllegalArgumentException("Tên đăng nhập từ 3 đến 50 ký tự, chỉ chứa chữ cái, số và dấu gạch dưới (_), không chứa dấu cách và không lặp ký tự liên tiếp quá 2 lần!");
        }
        if (userRepository.existsByUsername(cleanUsername)) {
            throw new IllegalArgumentException("Tên đăng nhập '" + cleanUsername + "' đã tồn tại trên hệ thống!");
        }

        String rawFullName = request.getFullName() != null ? request.getFullName().trim() : "";
        String cleanFullName = rawFullName.replaceAll("\\s+", " ");
        if (cleanFullName.isEmpty()) {
            throw new IllegalArgumentException("Họ và tên không được để trống!");
        }
        if (!cleanFullName.matches("^(?!.*(.)\\1{2,})[\\p{L}\\s\\-_']{2,100}$")) {
            throw new IllegalArgumentException("Họ và tên từ 2 đến 100 ký tự, chỉ chứa chữ cái, dấu cách, dấu gạch nối hoặc dấu nháy đơn, và không lặp ký tự liên tiếp quá 2 lần!");
        }

        String cleanEmail = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";
        if (cleanEmail.isEmpty()) {
            throw new IllegalArgumentException("Email không được để trống!");
        }
        if (userRepository.existsByEmail(cleanEmail)) {
            throw new IllegalArgumentException("Email '" + cleanEmail + "' đã được sử dụng bởi tài khoản khác!");
        }

        String cleanPhone = null;
        if (request.getPhone() != null && !request.getPhone().trim().isEmpty()) {
            cleanPhone = request.getPhone().replaceAll("[\\s\\-\\.]", "");
            if (!cleanPhone.matches("^(0|\\+84)[35789][0-9]{8}$")) {
                throw new IllegalArgumentException("Số điện thoại phải là số di động hợp lệ (VD: 0912345678) hoặc để trống!");
            }
        }

        String cleanPassword = request.getPassword() != null ? request.getPassword().trim() : "";
        if (!cleanPassword.matches("^(?=.*[A-Z])(?=.*[0-9])(?=.*[^a-zA-Z0-9]).{8,64}$")) {
            throw new IllegalArgumentException("Mật khẩu phải từ 8 đến 64 ký tự, bao gồm ít nhất 1 chữ hoa, 1 chữ số và 1 ký tự đặc biệt!");
        }

        if (request.getRoleIds() == null || request.getRoleIds().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ít nhất 1 vai trò cho người dùng!");
        }

        List<Role> roles = roleRepository.findAllById(request.getRoleIds());
        if (roles.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy vai trò hợp lệ trong hệ thống!");
        }

        boolean hasInactiveRole = roles.stream()
                .anyMatch(r -> !r.isActive());
        if (hasInactiveRole) {
            Role inactiveRole = roles.stream().filter(r -> !r.isActive()).findFirst().orElse(null);
            String name = inactiveRole != null ? inactiveRole.getRoleName().name() : "";
            throw new IllegalArgumentException("Vai trò [" + name + "] đang ở trạng thái ẩn hoặc tạm ngưng hoạt động, không thể tạo tài khoản!");
        }

        User newUser = new User();
        newUser.setUsername(cleanUsername);
        newUser.setPasswordHash(BCrypt.hashpw(request.getPassword().trim(), BCrypt.gensalt()));
        newUser.setEmail(cleanEmail);
        newUser.setFullName(cleanFullName);
        newUser.setPhone(cleanPhone);
        newUser.setStatus(request.getStatus() != null ? request.getStatus() : UserStatus.ACTIVE);
        newUser.setRoles(roles);

        newUser = userRepository.save(newUser);

        boolean hasCustomerRole = roles.stream()
                .anyMatch(r -> r.getRoleName() == RoleName.CUSTOMER);
        if (hasCustomerRole) {
            Cart cart = new Cart();
            cart.setCustomer(newUser);
            cartRepository.save(cart);

            MembershipTier defaultTier = tierRepository.findTopByOrderByMinSpentRequiredAsc();
            if (defaultTier != null) {
                CustomerMembership customerMembership = new CustomerMembership();
                customerMembership.setUser(newUser);
                customerMembership.setTier(defaultTier);
                customerMembership.setTotalCumulativeSpend(BigDecimal.ZERO);
                customerMembershipRepository.save(customerMembership);
            }
        }
    }

    private static final Set<String> ALLOWED_USER_SORT_FIELDS = Set.of(
            "userId", "username", "fullName", "email", "status", "createdAt"
    );

    @Override
    public Page<User> getAdminUserPage(String keyword, Long roleId, String status, int page, int size, String sortBy, String sortDir) {
        int pageIndex = Math.max(0, page - 1);
        int pageSize = size > 0 ? Math.min(size, 100) : 5;

        String safeSortBy = (sortBy != null && ALLOWED_USER_SORT_FIELDS.contains(sortBy.trim()))
                ? sortBy.trim() : "userId";
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
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
    public Map<String, Object> getAdminUserPageData(String keyword, Long roleId, String status, int page, int size, String sortBy, String sortDir) {
        String cleanKeyword = (keyword != null) ? keyword.trim() : "";
        String safeSortBy = (sortBy != null && ALLOWED_USER_SORT_FIELDS.contains(sortBy.trim()))
                ? sortBy.trim() : "userId";
        String safeSortDir = "desc".equalsIgnoreCase(sortDir) ? "desc" : "asc";
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
        data.put("newUser", new UserRequest());

        return data;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Integer userId, ChangePasswordRequest changePasswordRequest) throws Exception {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new Exception("Lỗi: Không tìm thấy người dùng trong hệ thống."));

        if (!BCrypt.checkpw(changePasswordRequest.getOldPassword(), user.getPasswordHash())) {
            throw new Exception("Mật khẩu cũ không chính xác.");
        }
        if (!changePasswordRequest.getNewPassword().equals(changePasswordRequest.getNewPasswordConfirm())) {
            throw new Exception("Mật khẩu xác nhận không trùng khớp.");
        }

        user.setPasswordHash(BCrypt.hashpw(changePasswordRequest.getNewPassword(), BCrypt.gensalt()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void editCustomerProfile(Integer userId, CustomerProfileRequest customerProfileRequest) throws Exception {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new Exception("Lỗi: Không tìm thấy người dùng trong hệ thống."));
        if(!user.getEmail().trim().equalsIgnoreCase(customerProfileRequest.getEmail())) {
            if(userRepository.existsByEmail(customerProfileRequest.getEmail())) {
                throw new Exception("Email đã tồn tại trong hệ thống.");
            }
            user.setEmail(customerProfileRequest.getEmail());
        }
        if(!user.getUsername().trim().equalsIgnoreCase(customerProfileRequest.getUsername())) {
            if(userRepository.existsByUsername(customerProfileRequest.getUsername())) {
                throw new Exception("Username đã tồn tại trong hệ thống.");
            }
            user.setUsername(customerProfileRequest.getUsername());
        }
        user.setFullName(customerProfileRequest.getFullName());
        user.setPhone(customerProfileRequest.getPhone());
        userRepository.save(user);
    }

}
