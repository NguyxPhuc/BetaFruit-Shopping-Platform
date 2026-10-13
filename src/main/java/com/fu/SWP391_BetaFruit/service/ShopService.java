package com.fu.SWP391_BetaFruit.service;

import com.fu.SWP391_BetaFruit.entity.Shop;
import com.fu.SWP391_BetaFruit.enums.ShopApprovalStatus;

import java.util.List;

public interface ShopService {
    Shop getShopById(Integer shopId);
    void approveShop(Integer shopId);
    void rejectShop(Integer shopId);
    void updateCommissionRate(Integer shopId, java.math.BigDecimal commissionRate);
    long countByStatus(ShopApprovalStatus status);
    long countTotal();
    java.util.Map<String, Object> getAdminShopPageData(String keyword, String statusStr, int page, int size, String sortBy, String sortDir);
    java.util.Map<String, Object> getAdminCommissionPageData(String keyword, String ownerStatus, String commissionTier, int page, int size, String sortBy, String sortDir);
}
