package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.dto.request.ReviewReportRequest;
import com.fu.SWP391_BetaFruit.entity.Review;
import com.fu.SWP391_BetaFruit.entity.ReviewReport;
import com.fu.SWP391_BetaFruit.entity.Shop;
import com.fu.SWP391_BetaFruit.entity.User;
import com.fu.SWP391_BetaFruit.enums.DisputeStatus;
import com.fu.SWP391_BetaFruit.enums.ReportReason;
import com.fu.SWP391_BetaFruit.enums.ReviewVisibility;
import com.fu.SWP391_BetaFruit.repository.ReviewReportRepository;
import com.fu.SWP391_BetaFruit.repository.ReviewRepository;
import com.fu.SWP391_BetaFruit.repository.ShopRepository;
import com.fu.SWP391_BetaFruit.repository.UserRepository;
import com.fu.SWP391_BetaFruit.service.ReviewReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReviewReportServiceImpl implements ReviewReportService {

    private final ReviewReportRepository reviewReportRepository;
    private final ReviewRepository reviewRepository;
    private final ShopRepository shopRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void createReviewReport(Integer userId, Integer reviewId, ReviewReportRequest request) {
        Shop shop = shopRepository.findByOwner_UserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin gian hàng của bạn!"));

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đánh giá cần khiếu nại!"));

        Integer productShopId = review.getOrderItem().getVariant().getProduct().getShop().getShopId();
        if (!productShopId.equals(shop.getShopId())) {
            throw new SecurityException("Bạn không có quyền gửi khiếu nại cho sản phẩm của gian hàng khác!");
        }

        boolean alreadyPending = reviewReportRepository.existsByReview_ReviewIdAndStatusIn(
                reviewId,
                Arrays.asList(DisputeStatus.PENDING, DisputeStatus.UNDER_REVIEW)
        );
        if (alreadyPending) {
            throw new IllegalStateException("Đánh giá này đang có khiếu nại chờ xử lý, vui lòng không gửi lại!");
        }

        ReviewReport report = new ReviewReport();
        report.setReview(review);
        report.setShop(shop);
        report.setReasonCategory(request.getReasonCategory());
        report.setExplanation(request.getExplanation());
        report.setEvidenceUrls(request.getEvidenceUrls());
        report.setStatus(DisputeStatus.PENDING);
        report.setCreatedAt(LocalDateTime.now());
        reviewReportRepository.save(report);

        review.setIsReported(true);
        review.setUpdatedAt(LocalDateTime.now());
        reviewRepository.save(review);
    }

    @Override
    @Transactional
    public void resolveReport(Integer reportId, String action, String staffNote, Integer staffId) {
        ReviewReport report = reviewReportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy yêu cầu khiếu nại ID: " + reportId));

        User staff = null;
        if (staffId != null) {
            staff = userRepository.findById(staffId).orElse(null);
        }

        report.setProcessedBy(staff);
        report.setResolvedAt(LocalDateTime.now());
        report.setStaffNote(staffNote != null ? staffNote.trim() : null);

        Review review = report.getReview();

        if ("APPROVE_HIDE".equalsIgnoreCase(action)) {
            report.setStatus(DisputeStatus.APPROVED_HIDDEN);
            review.setVisibilityStatus(ReviewVisibility.HIDDEN_BY_ADMIN);
            review.setIsReported(true);
        } else if ("REJECT_WITH_WARNING".equalsIgnoreCase(action)) {
            report.setStatus(DisputeStatus.REJECTED_WITH_WARNING);
            review.setIsReported(false);
        } else {
            report.setStatus(DisputeStatus.REJECTED);
            review.setIsReported(false);
        }

        review.setUpdatedAt(LocalDateTime.now());
        reviewRepository.save(review);
        reviewReportRepository.save(report);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getShopReviewsPageData(Integer userId, Integer rating, Boolean isReported, int page, int size) {
        Shop shop = shopRepository.findByOwner_UserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin gian hàng!"));

        int safePage = Math.max(page - 1, 0);
        Pageable pageable = PageRequest.of(safePage, size);
        Page<Review> reviewsPage = reviewRepository.findShopReviews(shop.getShopId(), rating, isReported, pageable);

        Map<String, Object> data = new HashMap<>();
        data.put("reviewsPage", reviewsPage);
        data.put("reviews", reviewsPage.getContent());
        data.put("currentPage", page);
        data.put("totalPages", reviewsPage.getTotalPages());
        data.put("totalItems", reviewsPage.getTotalElements());
        data.put("currentRating", rating);
        data.put("currentIsReported", isReported);
        data.put("reasons", ReportReason.values());
        data.put("reportRequest", new ReviewReportRequest());
        return data;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getStaffReportPageData(String statusStr, int page, int size) {
        int safePage = Math.max(page - 1, 0);
        Pageable pageable = PageRequest.of(safePage, size, Sort.by("createdAt").descending());

        Page<ReviewReport> reportsPage;
        String currentStatus = (statusStr != null && !statusStr.trim().isEmpty()) ? statusStr.trim().toUpperCase() : "PENDING";

        if ("ALL".equalsIgnoreCase(currentStatus)) {
            reportsPage = reviewReportRepository.findAll(pageable);
        } else {
            try {
                DisputeStatus status = DisputeStatus.valueOf(currentStatus);
                reportsPage = reviewReportRepository.findByStatus(status, pageable);
            } catch (IllegalArgumentException e) {
                reportsPage = reviewReportRepository.findByStatus(DisputeStatus.PENDING, pageable);
                currentStatus = "PENDING";
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("reportsPage", reportsPage);
        data.put("reports", reportsPage.getContent());
        data.put("currentStatus", currentStatus);
        data.put("pendingCount", reviewReportRepository.countByStatus(DisputeStatus.PENDING));
        data.put("approvedCount", reviewReportRepository.countByStatus(DisputeStatus.APPROVED_HIDDEN));
        data.put("rejectedCount", reviewReportRepository.countByStatus(DisputeStatus.REJECTED)
                + reviewReportRepository.countByStatus(DisputeStatus.REJECTED_WITH_WARNING));
        data.put("totalCount", reviewReportRepository.count());
        data.put("currentPage", page);
        data.put("totalPages", reportsPage.getTotalPages());
        data.put("totalItems", reportsPage.getTotalElements());
        return data;
    }

    @Override
    @Transactional(readOnly = true)
    public long countPendingReports() {
        return reviewReportRepository.countByStatus(DisputeStatus.PENDING);
    }
}
