package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.dto.request.LoginRequest;
import com.fu.SWP391_BetaFruit.dto.request.RegisterRequest;
import com.fu.SWP391_BetaFruit.dto.response.LoginResponse;
import com.fu.SWP391_BetaFruit.entity.*;
import com.fu.SWP391_BetaFruit.enums.NotificationType;
import com.fu.SWP391_BetaFruit.enums.UserStatus;
import com.fu.SWP391_BetaFruit.repository.*;
import com.fu.SWP391_BetaFruit.service.AuthService;
import com.fu.SWP391_BetaFruit.service.EmailService;
import com.fu.SWP391_BetaFruit.service.NotificationService;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private MembershipTierRepository tierRepository;

    @Autowired
    private CustomerMembershipRepository membershipRepository;

    @Autowired
    private EmailService  emailService;

    @Autowired
    private NotificationService notificationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void registerForCustomer(RegisterRequest registerRequest) throws Exception {
        if(userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new Exception("Username đã tồn tại trong hệ thống. Vui lòng đặt username khác.");
        }
        if(userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new Exception("Email đã tồn tại trong hệ thống. Vui lòng đặt email khác.");
        }
        User newUser = new User();
        newUser.setUsername(registerRequest.getUsername());
        newUser.setPasswordHash(BCrypt.hashpw(registerRequest.getPassword(), BCrypt.gensalt()));
        newUser.setEmail(registerRequest.getEmail());
        newUser.setFullName(registerRequest.getFullName());
        newUser.setPhone(registerRequest.getPhone());
        newUser.setStatus(UserStatus.ACTIVE);

        newUser = userRepository.save(newUser);

        Role customerRole = roleRepository.findByRoleName("Customer")
                .orElseThrow(() -> new Exception("Lỗi hệ thống: Không tìm quyền trong hệ thống."));
        newUser.getRoles().add(customerRole);
        userRepository.save(newUser);

        Cart cart = new Cart();
        cart.setCustomer(newUser);
        cartRepository.save(cart);

        MembershipTier defaultTier = tierRepository.findTopByOrderByMinSpentRequiredAsc();
        if(defaultTier != null) {
            CustomerMembership customerMembership = new CustomerMembership();
            customerMembership.setUser(newUser);
            customerMembership.setTier(defaultTier);
            customerMembership.setTotalCumulativeSpend(BigDecimal.ZERO);
            membershipRepository.save(customerMembership);
        }

        emailService.sendWelcomeEmail(registerRequest.getEmail(), registerRequest.getFullName());
        notificationService.createBaseNotification(
                newUser,
                "Chào mừng đến với BetaFruit",
                "Tài khoản khách hàng của bạn đã sẵn sàng. Khám phá các loại trái cây tươi ngon ngay hôm nay!",
                NotificationType.SYSTEM_ALERT,
                "/");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void registerForShopOwner(RegisterRequest registerRequest) throws Exception {
        if(userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new Exception("Username đã tồn tại trong hệ thống. Vui lòng đặt username khác.");
        }
        if(userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new Exception("Email đã tồn tại trong hệ thống. Vui lòng đặt email khác.");
        }

        User newUser = new User();
        newUser.setUsername(registerRequest.getUsername());
        newUser.setPasswordHash(BCrypt.hashpw(registerRequest.getPassword(), BCrypt.gensalt()));
        newUser.setEmail(registerRequest.getEmail());
        newUser.setFullName(registerRequest.getFullName());
        newUser.setPhone(registerRequest.getPhone());
        newUser.setStatus(UserStatus.ACTIVE);

        newUser = userRepository.save(newUser);

        Role ownerRole = roleRepository.findByRoleName("ShopOwner")
                .orElseThrow(() -> new Exception("Lỗi hệ thống: Không tìm thấy quyền trong hệ thống."));
        newUser.getRoles().add(ownerRole);
        userRepository.save(newUser);

        emailService.sendWelcomeEmail(registerRequest.getEmail(), registerRequest.getFullName());
        notificationService.createBaseNotification(
                newUser,
                "Chào mừng Đối tác BetaFruit",
                "Tài khoản chủ cửa hàng đã được tạo. Hãy hoàn thiện hồ sơ để bắt đầu đăng bán nông sản nhé!",
                NotificationType.SYSTEM_ALERT,
                "/shop/profile"
        );
    }

    @Override
    public LoginResponse login(LoginRequest loginRequest) throws Exception {
        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new Exception("Tài khoản không tồn tại trên hệ thống. Vui lòng đăng ký tài khoản."));

        boolean isMatchPassword = BCrypt.checkpw(loginRequest.getPassword(), user.getPasswordHash());
        if(!isMatchPassword) {
            throw new Exception("Sai mật khẩu. Vui lòng đăng nhập lại.");
        }
        if(user.getStatus() != UserStatus.ACTIVE) {
            throw new Exception("Tài khoản của bạn đã bị khóa hoặc chưa kích hoạt. Vui lòng liên hệ Admin.");
        }

        List<String> roles = new ArrayList<>();
        for(Role role : user.getRoles()) {
            roles.add(role.getRoleName());
        }

        LoginResponse loginResponse = new LoginResponse();
        loginResponse.setUserId(user.getUserId());
        loginResponse.setUsername(user.getUsername());
        loginResponse.setFullName(user.getFullName());
        loginResponse.setRoles(roles);

        return loginResponse;
    }
}
