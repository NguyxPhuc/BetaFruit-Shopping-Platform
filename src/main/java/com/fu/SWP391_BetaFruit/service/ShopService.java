package com.fu.SWP391_BetaFruit.service;

import com.fu.SWP391_BetaFruit.entity.Shop;
import com.fu.SWP391_BetaFruit.enums.ShopApprovalStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface ShopService {
    Shop getShopById(Integer shopId);
    void approveShop(Integer shopId);
    void rejectShop(Integer shopId);
    void updateCommissionRate(Integer shopId, BigDecimal commissionRate);
    long countByStatus(ShopApprovalStatus status);
    long countTotal();
    Map<String, Object> getAdminShopPageData(String keyword, String statusStr, int page, int size, String sortBy, String sortDir);
    Map<String, Object> getAdminCommissionPageData(String keyword, String ownerStatus, String commissionTier, int page, int size, String sortBy, String sortDir);
}
