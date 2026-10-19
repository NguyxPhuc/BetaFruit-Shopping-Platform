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
import com.fu.SWP391_BetaFruit.utils.StringUtils;
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
        String cleanEmail = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";

        // Bước 1: Kiểm tra ràng buộc duy nhất (DB)
        validateUniqueConstraints(cleanUsername, cleanEmail);

        // Bước 2: Kiểm tra và lấy danh sách vai trò hợp lệ
        List<Role> roles = validateAndGetRoles(request.getRoleIds());

        // Bước 3: Tạo và lưu thông tin người dùng
        User newUser = saveNewUser(request, cleanUsername, cleanEmail, roles);

        // Bước 4: Khởi tạo Giỏ hàng & Hạng thành viên (nếu là CUSTOMER)
        initCustomerBenefitsIfApplicable(newUser, roles);
    }

    private void validateUniqueConstraints(String username, String email) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Tên đăng nhập '" + username + "' đã tồn tại trên hệ thống!");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email '" + email + "' đã được sử dụng bởi tài khoản khác!");
        }
    }

    private List<Role> validateAndGetRoles(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ít nhất 1 vai trò cho người dùng!");
        }

        List<Role> roles = roleRepository.findAllById(roleIds);
        if (roles.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy vai trò hợp lệ trong hệ thống!");
        }

        for (Role role : roles) {
            if (!role.isActive()) {
                String name = role.getRoleName() != null ? role.getRoleName().name() : "";
                throw new IllegalArgumentException("Vai trò [" + name + "] đang ở trạng thái ẩn hoặc tạm ngưng hoạt động, không thể tạo tài khoản!");
            }
        }
        return roles;
    }

    private User saveNewUser(UserRequest request, String cleanUsername, String cleanEmail, List<Role> roles) {
        String rawFullName = request.getFullName() != null ? request.getFullName().trim() : "";
        String cleanFullName = rawFullName.replaceAll("\\s+", " ");

        String cleanPhone = null;
        if (request.getPhone() != null && !request.getPhone().trim().isEmpty()) {
            cleanPhone = request.getPhone().replaceAll("[\\s\\-\\.]", "");
        }

        String rawPassword = request.getPassword() != null ? request.getPassword().trim() : "";

        User newUser = new User();
        newUser.setUsername(cleanUsername);
        newUser.setPasswordHash(BCrypt.hashpw(rawPassword, BCrypt.gensalt()));
        newUser.setEmail(cleanEmail);
        newUser.setFullName(cleanFullName);
        newUser.setPhone(cleanPhone);
        newUser.setStatus(request.getStatus() != null ? request.getStatus() : UserStatus.ACTIVE);
        newUser.setRoles(roles);

        return userRepository.save(newUser);
    }

    private void initCustomerBenefitsIfApplicable(User user, List<Role> roles) {
        boolean hasCustomerRole = roles.stream()
                .anyMatch(r -> r.getRoleName() == RoleName.CUSTOMER);
        if (hasCustomerRole) {
            Cart cart = new Cart();
            cart.setCustomer(user);
            cartRepository.save(cart);

            MembershipTier defaultTier = tierRepository.findTopByOrderByMinSpentRequiredAsc();
            if (defaultTier != null) {
                CustomerMembership customerMembership = new CustomerMembership();
                customerMembership.setUser(user);
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
    public long countUsers() {
        return userRepository.count();
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
        int pageIndex = Math.max(0, page - 1);
        int pageSize = size > 0 ? Math.min(size, 100) : 5;

        String safeSortBy = (sortBy != null && ALLOWED_USER_SORT_FIELDS.contains(sortBy.trim()))
                ? sortBy.trim() : "userId";
        String safeSortDir = "desc".equalsIgnoreCase(sortDir) ? "desc" : "asc";
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(pageIndex, pageSize, Sort.by(direction, safeSortBy));

        String cleanKeyword = StringUtils.sanitizeKeyword(keyword);
        Long validRoleId = (roleId != null && roleId > 0) ? roleId : null;
        UserStatus filterStatus = parseUserStatus(status);

        Page<User> userPage = userRepository.filterUsers(cleanKeyword, validRoleId, filterStatus, pageable);
        List<Role> allRoles = roleRepository.findAll();

        Map<String, Object> data = new HashMap<>();
        data.put("users", userPage.getContent());
        data.put("userPage", userPage);
        data.put("pageData", userPage);
        data.put("allRoles", allRoles);
        data.put("keyword", cleanKeyword != null ? cleanKeyword : "");
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
