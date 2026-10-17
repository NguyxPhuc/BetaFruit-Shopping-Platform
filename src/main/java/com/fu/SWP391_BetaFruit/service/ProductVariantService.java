package com.fu.SWP391_BetaFruit.service;

import com.fu.SWP391_BetaFruit.dto.response.VariantResponse;

import java.util.List;

public interface ProductVariantService {
    List<VariantResponse> getAllVariantsByShopId(Integer userId) throws Exception;
}
