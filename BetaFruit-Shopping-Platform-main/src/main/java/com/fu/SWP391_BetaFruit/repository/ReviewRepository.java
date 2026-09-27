package com.fu.SWP391_BetaFruit.repository;

import com.fu.SWP391_BetaFruit.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Integer> {
    // ProductId is now reached through the purchased variant.
    List<Review> findByOrderItemVariantProductProductIdOrderByCreatedAtDescReviewIdDesc(Integer productId);
}
