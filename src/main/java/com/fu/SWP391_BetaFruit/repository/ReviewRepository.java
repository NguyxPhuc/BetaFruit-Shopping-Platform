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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Review r WHERE r.reviewId = :reviewId")
    Optional<Review> findLockedById(@Param("reviewId") Integer reviewId);

    @Query("SELECT r FROM Review r WHERE r.orderItem.variant.product.productId = :productId ")
    Optional<Review> findByOrderItem_OrderItemId(Integer orderItemId);

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
