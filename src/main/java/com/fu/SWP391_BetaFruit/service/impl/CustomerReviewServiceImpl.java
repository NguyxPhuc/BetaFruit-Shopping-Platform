package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.dto.request.CustomerReviewRequest;
import com.fu.SWP391_BetaFruit.entity.OrderItem;
import com.fu.SWP391_BetaFruit.entity.Review;
import com.fu.SWP391_BetaFruit.enums.OrderStatus;
import com.fu.SWP391_BetaFruit.enums.ReviewVisibility;
import com.fu.SWP391_BetaFruit.repository.OrderItemRepository;
import com.fu.SWP391_BetaFruit.repository.ReviewRepository;
import com.fu.SWP391_BetaFruit.service.CustomerReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CustomerReviewServiceImpl implements CustomerReviewService {
    private final OrderItemRepository orderItemRepository;
    private final ReviewRepository reviewRepository;

    @Override
    @Transactional
    public void createReview(Integer customerId, CustomerReviewRequest request) {
        OrderItem item = eligibleItem(customerId, request.orderId(), request.orderItemId(), request.productId());
        if (reviewRepository.findByOrderItem_OrderItemId(item.getOrderItemId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Sản phẩm trong đơn hàng này đã được đánh giá");
        }

        Review review = new Review();
        review.setOrderItem(item);
        review.setCustomer(item.getOrder().getCustomer());
        review.setRating(request.rating());
        review.setComment(normalizeComment(request.comment()));
        review.setVisibilityStatus(ReviewVisibility.VISIBLE);
        review.setIsReported(false);
        review.setCreatedAt(LocalDateTime.now());
        reviewRepository.save(review);
    }

    @Override
    @Transactional
    public void updateReview(Integer customerId, Integer reviewId, CustomerReviewRequest request) {
        Review review = reviewRepository.findByIdForUpdate(reviewId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đánh giá"));
        if (!Objects.equals(review.getCustomer().getUserId(), customerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền chỉnh sửa đánh giá này");
        }
        OrderItem item = eligibleItem(customerId, request.orderId(), request.orderItemId(), request.productId());
        if (!Objects.equals(review.getOrderItem().getOrderItemId(), item.getOrderItemId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Review không khớp với sản phẩm trong đơn hàng");
        }
        ensureCustomerMayEdit(review);
        if (hasCustomerUsedEdit(review)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bạn đã sử dụng lượt sửa duy nhất cho đánh giá này");
        }

        review.setRating(request.rating());
        review.setComment(normalizeComment(request.comment()));
        if (review.getVisibilityStatus() == ReviewVisibility.HIDDEN_BY_USER) {
            review.setVisibilityStatus(ReviewVisibility.VISIBLE);
        }
        LocalDateTime editedAt = LocalDateTime.now();
        if (review.getCreatedAt() != null && !editedAt.isAfter(review.getCreatedAt().plusSeconds(2))) {
            editedAt = review.getCreatedAt().plusSeconds(3);
        }
        review.setUpdatedAt(editedAt);
        reviewRepository.save(review);
    }

    @Override
    @Transactional
    public void hideReview(Integer customerId, Integer reviewId) {
        Review review = ownedReview(customerId, reviewId);
        ensureCustomerMayEdit(review);
        review.setVisibilityStatus(ReviewVisibility.HIDDEN_BY_USER);
        reviewRepository.save(review);
    }

    private OrderItem eligibleItem(Integer customerId, Integer orderId, Integer orderItemId, Integer productId) {
        OrderItem item = orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm trong đơn hàng"));
        if (!Objects.equals(item.getOrder().getCustomer().getUserId(), customerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền đánh giá sản phẩm trong đơn hàng này");
        }
        if (!Objects.equals(item.getOrder().getOrderId(), orderId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sản phẩm không khớp với đơn hàng");
        }
        if (!Objects.equals(item.getVariant().getProduct().getProductId(), productId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sản phẩm không khớp với sản phẩm trong đơn hàng");
        }
        OrderStatus status = item.getOrder().getOrderStatus();
        if (status != OrderStatus.RECEIVED && status != OrderStatus.SUCCESS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Chỉ có thể đánh giá sau khi đã xác nhận nhận hàng");
        }
        return item;
    }

    private Review ownedReview(Integer customerId, Integer reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đánh giá"));
        if (!Objects.equals(review.getCustomer().getUserId(), customerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền chỉnh sửa đánh giá này");
        }
        return review;
    }

    private void ensureCustomerMayEdit(Review review) {
        if (review.getVisibilityStatus() == ReviewVisibility.HIDDEN_BY_ADMIN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Đánh giá đã bị quản trị viên ẩn và không thể chỉnh sửa");
        }
    }

    private boolean hasCustomerUsedEdit(Review review) {
        return review.getUpdatedAt() != null
                && review.getCreatedAt() != null
                && review.getUpdatedAt().isAfter(review.getCreatedAt().plusSeconds(2));
    }

    private String normalizeComment(String comment) {
        if (comment == null || comment.isBlank()) return null;
        return comment.trim();
    }

}
