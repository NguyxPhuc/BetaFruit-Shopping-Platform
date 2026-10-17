package com.fu.SWP391_BetaFruit.service;

import com.fu.SWP391_BetaFruit.dto.request.ProductCreateRequest;
import com.fu.SWP391_BetaFruit.dto.request.ProductUpdateRequest;
import com.fu.SWP391_BetaFruit.entity.Product;
import com.fu.SWP391_BetaFruit.entity.Shop;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

public interface ProductService {
    Product getProductById(Integer productId);
    void approveProduct(Integer productId);
    void rejectProduct(Integer productId);
    void toggleProductVisibility(Integer productId);
    long countTotal();
    long countPending();
    long countActive();
    long countHidden();
    long countRejected();
    Page<Product> searchProductsPaginated(String keyword, String tab, int page, int size, String sortBy, String sortDir);
    Map<String, Object> getAdminProductPageData(String keyword, String tab, int page, int size, String sortBy, String sortDir);
    Product createProductForShop(Integer userId, ProductCreateRequest request);
    Page<Product> getShopProductsPaginated(Integer userId, String keyword, int page, int size);
    Shop getShopByUserId(Integer userId);
    void toggleProductVisibilityForShop(Integer userId, Integer productId);
    Product getShopProductForEdit(Integer userId, Integer productId);
    Product updateProductForShop(Integer userId, Integer productId, ProductUpdateRequest request);
}
