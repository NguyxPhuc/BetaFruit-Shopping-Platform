package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.dto.request.RegisterRequest;
import com.fu.SWP391_BetaFruit.entity.*;
import com.fu.SWP391_BetaFruit.enums.UserStatus;
import com.fu.SWP391_BetaFruit.repository.*;
import com.fu.SWP391_BetaFruit.service.AuthService;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

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

    @Override
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
    }
}
