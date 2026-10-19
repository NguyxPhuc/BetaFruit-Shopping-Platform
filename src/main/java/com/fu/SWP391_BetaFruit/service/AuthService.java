package com.fu.SWP391_BetaFruit.service;

import com.fu.SWP391_BetaFruit.dto.request.LoginRequest;
import com.fu.SWP391_BetaFruit.dto.request.RegisterRequest;
import com.fu.SWP391_BetaFruit.dto.response.LoginResponse;

public interface AuthService {

    public void registerForCustomer(RegisterRequest registerRequest) throws Exception;

    public void registerForShopOwner(RegisterRequest registerRequest) throws Exception;

    public LoginResponse login(LoginRequest loginRequest) throws Exception;
}
