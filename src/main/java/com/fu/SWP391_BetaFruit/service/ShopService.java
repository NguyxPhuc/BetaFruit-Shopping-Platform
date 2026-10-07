package com.fu.SWP391_BetaFruit.service;

import com.fu.SWP391_BetaFruit.entity.Shop;
import com.fu.SWP391_BetaFruit.enums.ShopApprovalStatus;

import java.util.List;

public interface ShopService {
    List<Shop> getAllShops();
    List<Shop> getShopsByStatus(ShopApprovalStatus status);
    List<Shop> searchShops(String keyword, ShopApprovalStatus status);
    Shop getShopById(Integer shopId);
    void approveShop(Integer shopId);
    void rejectShop(Integer shopId);
    void updateCommissionRate(Integer shopId, java.math.BigDecimal commissionRate);
    long countByStatus(ShopApprovalStatus status);
    long countTotal();
    org.springframework.data.domain.Page<Shop> searchShopsPaginated(String keyword, ShopApprovalStatus status, int page, int size);
    org.springframework.data.domain.Page<Shop> searchShopsPaginated(String keyword, ShopApprovalStatus status, int page, int size, String sortBy, String sortDir);
    java.util.Map<String, Object> getAdminShopPageData(String keyword, String statusStr);
    java.util.Map<String, Object> getAdminShopPageData(String keyword, String statusStr, int page, int size);
    java.util.Map<String, Object> getAdminShopPageData(String keyword, String statusStr, int page, int size, String sortBy, String sortDir);
    java.util.Map<String, Object> getAdminCommissionPageData(String keyword, String ownerStatus, String commissionTier, int page, int size, String sortBy, String sortDir);
}
