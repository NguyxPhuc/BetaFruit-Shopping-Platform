package com.fu.SWP391_BetaFruit.service;

import com.fu.SWP391_BetaFruit.dto.response.SystemAnalyticsResponse;

public interface AdminAnalyticsService {
    /**
     * Lấy dữ liệu tổng quan phân tích hệ thống dành cho Quản trị viên (System Analytics)
     * Bao gồm:
     * - Total revenue (Tổng doanh thu toàn sàn & Phí sàn)
     * - Total Orders (Tổng số đơn hàng toàn sàn)
     * - Total Shop Owner (Tổng số lượng chủ gian hàng)
     * - Total Customer (Tổng số lượng khách hàng)
     * - Top 5 Best Selling Product (Top 5 sản phẩm bán chạy nhất)
     */
    SystemAnalyticsResponse getSystemAnalytics();
}

