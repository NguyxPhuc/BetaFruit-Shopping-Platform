package com.fu.SWP391_BetaFruit.repository;

import com.fu.SWP391_BetaFruit.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Integer> {

    @Query("SELECT r FROM Review r WHERE r.orderItem.variant.product.productId = :productId " +
           "AND (r.visibilityStatus IS NULL OR r.visibilityStatus = com.fu.SWP391_BetaFruit.enums.ReviewVisibility.VISIBLE) " +
           "ORDER BY r.createdAt DESC")
    List<Review> findByProductId(@Param("productId") Integer productId);

    @Query("SELECT AVG(CAST(r.rating AS double)) FROM Review r WHERE r.orderItem.variant.product.productId = :productId " +
           "AND (r.visibilityStatus IS NULL OR r.visibilityStatus = com.fu.SWP391_BetaFruit.enums.ReviewVisibility.VISIBLE)")
    Double getAverageRatingByProductId(@Param("productId") Integer productId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.orderItem.variant.product.productId = :productId " +
           "AND (r.visibilityStatus IS NULL OR r.visibilityStatus = com.fu.SWP391_BetaFruit.enums.ReviewVisibility.VISIBLE)")
    Long countReviewsByProductId(@Param("productId") Integer productId);
}
