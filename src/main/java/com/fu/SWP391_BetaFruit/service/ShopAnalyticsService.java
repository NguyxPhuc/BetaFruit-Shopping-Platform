package com.fu.SWP391_BetaFruit.service;

import com.fu.SWP391_BetaFruit.dto.response.ShopAnalyticsResponse;

public interface ShopAnalyticsService {
    /**
     * Lấy toàn bộ chỉ số thống kê bán hàng, doanh thu, đơn hàng, khách hàng
     * và top sản phẩm bán chạy nhất của gian hàng theo ID của chủ shop.
     *
     * @param ownerUserId ID tài khoản chủ gian hàng
     * @return ShopAnalyticsResponse dữ liệu thống kê chi tiết
     */
    ShopAnalyticsResponse getShopAnalyticsForOwner(Integer ownerUserId);
}

