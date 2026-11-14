package com.fu.SWP391_BetaFruit.repository;

import com.fu.SWP391_BetaFruit.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Integer> {

    // Khi truy vấn trong transaction, lấy khóa ghi cho bản ghi review có reviewId tương ứng.
    // Giữ khóa suốt bước đọc, kiểm tra và cập nhật; chỉ thả khi transaction commit hoặc rollback.
    // Thao tác khác muốn lấy khóa ghi trên cùng review phải chờ để tránh ghi đè dữ liệu.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Review r WHERE r.reviewId = :reviewId")
    Optional<Review> findLockedById(@Param("reviewId") Integer reviewId);

    Optional<Review> findByOrderItem_OrderItemId(Integer orderItemId);

    List<Review> findByOrderItem_Order_OrderId(Integer orderId);

    // Cơ chế khóa giống findLockedById: giữ khóa bản ghi review đến khi transaction kết thúc.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Review r WHERE r.reviewId = :reviewId")
    Optional<Review> findByIdForUpdate(@Param("reviewId") Integer reviewId);

    @Query(value = "SELECT r FROM Review r WHERE r.orderItem.variant.product.productId = :productId " +
           "AND (r.visibilityStatus IS NULL OR r.visibilityStatus = com.fu.SWP391_BetaFruit.enums.ReviewVisibility.VISIBLE) " +
           "ORDER BY r.createdAt DESC",
           countQuery = "SELECT COUNT(r) FROM Review r WHERE r.orderItem.variant.product.productId = :productId " +
                   "AND (r.visibilityStatus IS NULL OR r.visibilityStatus = com.fu.SWP391_BetaFruit.enums.ReviewVisibility.VISIBLE)")
    Page<Review> findByProductId(@Param("productId") Integer productId, Pageable pageable);

    @Query("SELECT AVG(CAST(r.rating AS double)) FROM Review r WHERE r.orderItem.variant.product.productId = :productId " +
           "AND (r.visibilityStatus IS NULL OR r.visibilityStatus = com.fu.SWP391_BetaFruit.enums.ReviewVisibility.VISIBLE)")
    Double getAverageRatingByProductId(@Param("productId") Integer productId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.orderItem.variant.product.productId = :productId " +
           "AND (r.visibilityStatus IS NULL OR r.visibilityStatus = com.fu.SWP391_BetaFruit.enums.ReviewVisibility.VISIBLE)")
    Long countReviewsByProductId(@Param("productId") Integer productId);

    @Query("SELECT r FROM Review r WHERE r.orderItem.variant.product.shop.shopId = :shopId " +
           "AND (:rating IS NULL OR r.rating = :rating) " +
           "AND (:isReported IS NULL OR COALESCE(r.isReported, false) = :isReported) " +
           "ORDER BY r.createdAt DESC")
    Page<Review> findShopReviews(
            @Param("shopId") Integer shopId,
            @Param("rating") Integer rating,
            @Param("isReported") Boolean isReported,
            Pageable pageable);
}
