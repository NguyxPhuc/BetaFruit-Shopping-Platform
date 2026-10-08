package com.fu.SWP391_BetaFruit.service;

import com.fu.SWP391_BetaFruit.entity.Product;

import java.util.List;

public interface ProductService {
    List<Product> getAllProducts();
    List<Product> searchProducts(String keyword, String tab);
    Product getProductById(Integer productId);
    void approveProduct(Integer productId);
    void rejectProduct(Integer productId);
    void toggleProductVisibility(Integer productId);
    long countTotal();
    long countPending();
    long countActive();
    long countHidden();
    long countRejected();
    org.springframework.data.domain.Page<Product> searchProductsPaginated(String keyword, String tab, int page, int size);
    org.springframework.data.domain.Page<Product> searchProductsPaginated(String keyword, String tab, int page, int size, String sortBy, String sortDir);
    java.util.Map<String, Object> getAdminProductPageData(String keyword, String tab);
    java.util.Map<String, Object> getAdminProductPageData(String keyword, String tab, int page, int size);
    java.util.Map<String, Object> getAdminProductPageData(String keyword, String tab, int page, int size, String sortBy, String sortDir);
}
