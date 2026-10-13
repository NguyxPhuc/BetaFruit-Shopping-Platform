package com.fu.SWP391_BetaFruit.service;

import com.fu.SWP391_BetaFruit.dto.request.customer.ProductFilterRequest;
import com.fu.SWP391_BetaFruit.dto.response.customer.CustomerProductCardResponse;
import com.fu.SWP391_BetaFruit.dto.response.customer.CustomerProductDetailResponse;
import com.fu.SWP391_BetaFruit.dto.response.customer.CustomerProductPageResponse;
import com.fu.SWP391_BetaFruit.entity.MasterCategory;

import java.util.List;

public interface CustomerProductService {
    CustomerProductPageResponse getProductsPage(ProductFilterRequest request);
    List<CustomerProductCardResponse> getFeaturedProducts(int limit);
    CustomerProductDetailResponse getProductDetail(Integer productId);
    List<MasterCategory> getAllActiveCategories();
}
