package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.entity.Review;
import com.fu.SWP391_BetaFruit.entity.Shop;
import com.fu.SWP391_BetaFruit.repository.ReviewRepository;
import com.fu.SWP391_BetaFruit.repository.ShopRepository;
import com.fu.SWP391_BetaFruit.service.ShopReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ShopReviewServiceImpl implements ShopReviewService {
    private final ReviewRepository reviewRepository;
    private final ShopRepository shopRepository;

    @Override
    @Transactional
    public void saveReply(Integer ownerId, Integer reviewId, String reply) {
        Shop shop = shopRepository.findByOwner_UserId(ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy gian hàng của bạn."));
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đánh giá."));

        Integer reviewShopId = review.getOrderItem().getVariant().getProduct().getShop().getShopId();
        if (!shop.getShopId().equals(reviewShopId)) {
            throw new SecurityException("Bạn không có quyền phản hồi đánh giá của gian hàng khác.");
        }

        if (reply == null || reply.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập nội dung phản hồi.");
        }
        String normalizedReply = reply.trim();
        if (normalizedReply.length() > 5000) {
            throw new IllegalArgumentException("Phản hồi tối đa 5.000 ký tự.");
        }

        // Saving again replaces the shop's previous reply; review content and visibility stay untouched.
        review.setShopReply(normalizedReply);
        review.setRepliedAt(LocalDateTime.now());
        reviewRepository.save(review);
    }
}
