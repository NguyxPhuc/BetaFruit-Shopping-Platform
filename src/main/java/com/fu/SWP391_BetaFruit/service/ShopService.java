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
    long countByStatus(ShopApprovalStatus status);
    long countTotal();
}
