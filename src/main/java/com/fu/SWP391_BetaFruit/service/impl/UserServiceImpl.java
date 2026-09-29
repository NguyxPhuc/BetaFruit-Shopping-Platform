package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.dto.response.CustomerProfileResponse;
import com.fu.SWP391_BetaFruit.dto.response.ShopProfileResponse;
import com.fu.SWP391_BetaFruit.entity.CustomerMembership;
import com.fu.SWP391_BetaFruit.entity.Shop;
import com.fu.SWP391_BetaFruit.entity.User;
import com.fu.SWP391_BetaFruit.repository.CustomerMembershipRepository;
import com.fu.SWP391_BetaFruit.repository.ShopRepository;
import com.fu.SWP391_BetaFruit.repository.UserRepository;
import com.fu.SWP391_BetaFruit.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ShopRepository shopRepository;

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
}
