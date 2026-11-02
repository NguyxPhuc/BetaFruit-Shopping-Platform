package com.fu.SWP391_BetaFruit.service;

import com.fu.SWP391_BetaFruit.dto.request.ReviewReportRequest;
import com.fu.SWP391_BetaFruit.entity.ReviewReport;

import java.util.Map;

public interface ReviewReportService {

    void createReviewReport(Integer userId, Integer reviewId, ReviewReportRequest request);

    void resolveReport(Integer reportId, String action, String staffNote, Integer staffId);

    Map<String, Object> getShopReviewsPageData(Integer userId, Integer rating, Boolean isReported, int page, int size);

    Map<String, Object> getStaffReportPageData(String statusStr, int page, int size);

    long countPendingReports();
}
