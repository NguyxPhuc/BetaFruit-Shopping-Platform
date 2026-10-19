package com.fu.SWP391_BetaFruit.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerReviewResponse {
    private Integer reviewId;
    private String customerName;
    private Integer rating;
    private String comment;
    private String shopReply;
    private LocalDateTime createdAt;
    private LocalDateTime repliedAt;
    private String formattedDate;
}
