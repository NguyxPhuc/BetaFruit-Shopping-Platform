package com.fu.SWP391_BetaFruit.service;

import com.fu.SWP391_BetaFruit.dto.request.RegisterRequest;

public interface AuthService {

    public void registerForCustomer(RegisterRequest registerRequest) throws Exception;

}
