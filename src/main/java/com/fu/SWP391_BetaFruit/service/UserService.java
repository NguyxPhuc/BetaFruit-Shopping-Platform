package com.fu.SWP391_BetaFruit.service;

import com.fu.SWP391_BetaFruit.dto.response.CustomerProfileResponse;
import com.fu.SWP391_BetaFruit.dto.response.ShopProfileResponse;

public interface UserService {
    CustomerProfileResponse getCustomerProfile(Integer id) throws Exception;

    ShopProfileResponse getShopProfile(Integer id) throws Exception;
}
