package com.fu.SWP391_BetaFruit.service;

import com.fu.SWP391_BetaFruit.dto.response.SystemAnalyticsResponse;

public interface AdminAnalyticsService {
    /**
     * Lấy dữ liệu tổng quan phân tích hệ thống dành cho Quản trị viên (System Analytics).
     * Mặc định phân tích toàn sàn.
     */
    SystemAnalyticsResponse getSystemAnalytics();

    /**
     * Lấy dữ liệu phân tích hệ thống dành cho Quản trị viên theo từng gian hàng hoặc toàn sàn.
     * @param shopId Mã ID của cửa hàng cần phân tích (null nếu xem toàn sàn)
     */
    SystemAnalyticsResponse getSystemAnalytics(Integer shopId);
}
