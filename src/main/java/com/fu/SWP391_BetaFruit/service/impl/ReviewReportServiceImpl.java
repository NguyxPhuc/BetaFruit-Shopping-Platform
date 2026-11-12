package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.dto.request.ReviewReportRequest;
import com.fu.SWP391_BetaFruit.entity.Review;
import com.fu.SWP391_BetaFruit.entity.ReviewReport;
import com.fu.SWP391_BetaFruit.entity.Shop;
import com.fu.SWP391_BetaFruit.entity.User;
import com.fu.SWP391_BetaFruit.enums.DisputeStatus;
import com.fu.SWP391_BetaFruit.enums.ReportReason;
import com.fu.SWP391_BetaFruit.enums.ReviewVisibility;
import com.fu.SWP391_BetaFruit.enums.RoleName;
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
import java.net.URI;
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

        Review review = reviewRepository.findLockedById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đánh giá cần khiếu nại!"));

        Integer productShopId = review.getOrderItem().getVariant().getProduct().getShop().getShopId();
        if (!productShopId.equals(shop.getShopId())) {
            throw new SecurityException("Bạn không có quyền gửi khiếu nại cho sản phẩm của gian hàng khác!");
        }

        if (review.getVisibilityStatus() == ReviewVisibility.HIDDEN_BY_ADMIN) {
            throw new IllegalStateException("Đánh giá này đã được ẩn bởi quản trị viên!");
        }
        if (request == null || request.getReasonCategory() == null || request.getExplanation() == null
                || request.getExplanation().trim().length() < 10 || request.getExplanation().trim().length() > 1000) {
            throw new IllegalArgumentException("Vui lòng chọn lý do và nhập lời giải trình từ 10 đến 1000 ký tự!");
        }
        String evidenceUrl = request.getEvidenceUrls() == null ? null : request.getEvidenceUrls().trim();
        if (evidenceUrl != null && !evidenceUrl.isEmpty()) {
            try {
                URI uri = URI.create(evidenceUrl);
                if (evidenceUrl.length() > 2000 || uri.getHost() == null
                        || !("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))) {
                    throw new IllegalArgumentException();
                }
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Bằng chứng phải là đường dẫn HTTP hoặc HTTPS hợp lệ!");
            }
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
        report.setExplanation(request.getExplanation().trim());
        report.setEvidenceUrls(evidenceUrl);
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
        User staff = staffId == null ? null : userRepository.findById(staffId).orElse(null);
        if (staff == null || staff.getRoles() == null || staff.getRoles().stream().noneMatch(role ->
                role.getRoleName() == RoleName.ADMIN || role.getRoleName() == RoleName.STAFF)) {
            throw new SecurityException("Bạn không có quyền kiểm duyệt đánh giá!");
        }
        String normalizedAction = action == null ? "" : action.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("APPROVE_HIDE", "REJECT", "REJECT_WITH_WARNING").contains(normalizedAction)) {
            throw new IllegalArgumentException("Phán quyết không hợp lệ!");
        }
        if (staffNote != null && staffNote.trim().length() > 500) {
            throw new IllegalArgumentException("Ghi chú xử lý không được vượt quá 500 ký tự!");
        }
        // Lock the review before reading its report: reporting and moderation use the same lock.
        Integer reviewId = reviewReportRepository.findReviewIdByReportId(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy yêu cầu khiếu nại ID: " + reportId));
        Review review = reviewRepository.findLockedById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đánh giá!"));
        ReviewReport report = reviewReportRepository.findById(reportId).orElseThrow();
        if (report.getStatus() != DisputeStatus.PENDING && report.getStatus() != DisputeStatus.UNDER_REVIEW) {
            throw new IllegalStateException("Khiếu nại này đã được xử lý, không thể thay đổi phán quyết!");
        }

        report.setProcessedBy(staff);
        report.setResolvedAt(LocalDateTime.now());
        report.setStaffNote(staffNote != null ? staffNote.trim() : null);

        if ("APPROVE_HIDE".equals(normalizedAction)) {
            report.setStatus(DisputeStatus.APPROVED_HIDDEN);
            review.setVisibilityStatus(ReviewVisibility.HIDDEN_BY_ADMIN);
            review.setIsReported(true);
        } else if ("REJECT_WITH_WARNING".equals(normalizedAction)) {
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
        Pageable pageable = PageRequest.of(safePage, Math.min(Math.max(size, 1), 100));
        Page<Review> reviewsPage = reviewRepository.findShopReviews(shop.getShopId(), rating, isReported, pageable);

        Map<String, Object> data = new HashMap<>();
        data.put("reviewsPage", reviewsPage);
        data.put("reviews", reviewsPage.getContent());
        data.put("currentPage", safePage + 1);
        data.put("pageSize", pageable.getPageSize());
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
        Pageable pageable = PageRequest.of(safePage, Math.min(Math.max(size, 1), 100),
                Sort.by("createdAt").descending().and(Sort.by("reportId").descending()));

        Page<ReviewReport> reportsPage;
        String currentStatus = (statusStr != null && !statusStr.trim().isEmpty()) ? statusStr.trim().toUpperCase(Locale.ROOT) : "PENDING";

        if ("ALL".equalsIgnoreCase(currentStatus)) {
            reportsPage = reviewReportRepository.findAll(pageable);
        } else if ("REJECTED".equals(currentStatus)) {
            reportsPage = reviewReportRepository.findByStatusIn(
                    List.of(DisputeStatus.REJECTED, DisputeStatus.REJECTED_WITH_WARNING), pageable);
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
        data.put("underReviewCount", reviewReportRepository.countByStatus(DisputeStatus.UNDER_REVIEW));
        data.put("approvedCount", reviewReportRepository.countByStatus(DisputeStatus.APPROVED_HIDDEN));
        data.put("rejectedCount", reviewReportRepository.countByStatus(DisputeStatus.REJECTED)
                + reviewReportRepository.countByStatus(DisputeStatus.REJECTED_WITH_WARNING));
        data.put("totalCount", reviewReportRepository.count());
        data.put("currentPage", safePage + 1);
        data.put("pageSize", pageable.getPageSize());
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
