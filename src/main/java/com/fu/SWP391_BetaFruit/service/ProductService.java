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
}
