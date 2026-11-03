package com.fu.SWP391_BetaFruit.service;

import com.fu.SWP391_BetaFruit.dto.request.CustomerReviewRequest;

public interface CustomerReviewService {
    void createReview(Integer customerId, CustomerReviewRequest request);
    void updateReview(Integer customerId, Integer reviewId, CustomerReviewRequest request);
    void hideReview(Integer customerId, Integer reviewId);
}
