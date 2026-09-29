package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.entity.Product;
import com.fu.SWP391_BetaFruit.enums.ProductApprovalStatus;
import com.fu.SWP391_BetaFruit.enums.ProductVisibility;
import com.fu.SWP391_BetaFruit.repository.ProductRepository;
import com.fu.SWP391_BetaFruit.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    @Override
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Override
    public List<Product> searchProducts(String keyword, String tab) {
        String cleanKeyword = (keyword != null) ? keyword.trim() : "";
        boolean hasKeyword = !cleanKeyword.isEmpty();
        String currentTab = (tab != null && !tab.trim().isEmpty()) ? tab.trim().toUpperCase() : "ALL";

        return switch (currentTab) {
            case "PENDING" -> hasKeyword
                    ? productRepository.searchByApprovalStatus(cleanKeyword, ProductApprovalStatus.PENDING)
                    : productRepository.findByApprovalStatus(ProductApprovalStatus.PENDING);
            case "ACTIVE" -> hasKeyword
                    ? productRepository.searchByApprovalAndVisibility(cleanKeyword, ProductApprovalStatus.APPROVED, ProductVisibility.ACTIVE)
                    : productRepository.findByApprovalStatusAndVisibilityStatus(ProductApprovalStatus.APPROVED, ProductVisibility.ACTIVE);
            case "HIDDEN" -> hasKeyword
                    ? productRepository.searchByApprovalAndVisibility(cleanKeyword, ProductApprovalStatus.APPROVED, ProductVisibility.HIDDEN)
                    : productRepository.findByApprovalStatusAndVisibilityStatus(ProductApprovalStatus.APPROVED, ProductVisibility.HIDDEN);
            case "REJECTED" -> hasKeyword
                    ? productRepository.searchByApprovalStatus(cleanKeyword, ProductApprovalStatus.REJECTED)
                    : productRepository.findByApprovalStatus(ProductApprovalStatus.REJECTED);
            default -> hasKeyword
                    ? productRepository.searchAll(cleanKeyword)
                    : productRepository.findAll();
        };
    }

    @Override
    public Product getProductById(Integer productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm hoa quả với mã ID: " + productId));
    }

    @Override
    @Transactional
    public void approveProduct(Integer productId) {
        Product product = getProductById(productId);
        product.setApprovalStatus(ProductApprovalStatus.APPROVED);
        product.setVisibilityStatus(ProductVisibility.ACTIVE);
        productRepository.save(product);
    }

    @Override
    @Transactional
    public void rejectProduct(Integer productId) {
        Product product = getProductById(productId);
        product.setApprovalStatus(ProductApprovalStatus.REJECTED);
        product.setVisibilityStatus(ProductVisibility.HIDDEN);
        productRepository.save(product);
    }

    @Override
    @Transactional
    public void toggleProductVisibility(Integer productId) {
        Product product = getProductById(productId);
        if (product.getVisibilityStatus() == ProductVisibility.ACTIVE) {
            product.setVisibilityStatus(ProductVisibility.HIDDEN);
        } else {
            product.setVisibilityStatus(ProductVisibility.ACTIVE);
        }
        productRepository.save(product);
    }

    @Override
    public long countTotal() {
        return productRepository.count();
    }

    @Override
    public long countPending() {
        return productRepository.countByApprovalStatus(ProductApprovalStatus.PENDING);
    }

    @Override
    public long countActive() {
        return productRepository.countByApprovalStatusAndVisibilityStatus(ProductApprovalStatus.APPROVED, ProductVisibility.ACTIVE);
    }

    @Override
    public long countHidden() {
        return productRepository.countByApprovalStatusAndVisibilityStatus(ProductApprovalStatus.APPROVED, ProductVisibility.HIDDEN);
    }

    @Override
    public long countRejected() {
        return productRepository.countByApprovalStatus(ProductApprovalStatus.REJECTED);
    }

    @Override
    public Map<String, Object> getAdminProductPageData(String keyword, String tab) {
        String currentTab = (tab != null && !tab.trim().isEmpty()) ? tab.trim().toUpperCase() : "ALL";
        String cleanKeyword = (keyword != null) ? keyword.trim() : "";
        List<Product> products = searchProducts(cleanKeyword, currentTab);

        Map<String, Object> data = new HashMap<>();
        data.put("products", products);
        data.put("currentTab", currentTab);
        data.put("keyword", cleanKeyword);
        data.put("totalCount", countTotal());
        data.put("pendingCount", countPending());
        data.put("activeCount", countActive());
        data.put("hiddenCount", countHidden());
        data.put("rejectedCount", countRejected());

        return data;
    }
}
