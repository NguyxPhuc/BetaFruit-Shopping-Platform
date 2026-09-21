package com.fu.SWP391_BetaFruit.entity;

import com.fu.SWP391_BetaFruit.enums.ReviewVisibility;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "Review")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ReviewId")
    private Integer reviewId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OrderItemId", nullable = false, unique = true)
    private OrderItem orderItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CustomerId", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ProductId", nullable = false)
    private Product product;

    @Column(name = "Rating", nullable = false)
    private Integer rating;

    @Column(name = "Comment", columnDefinition = "NVARCHAR(MAX)")
    private String comment;

    @Column(name = "ShopReply", columnDefinition = "NVARCHAR(MAX)")
    private String shopReply;

    @Enumerated(EnumType.STRING)
    @Column(name = "VisibilityStatus", length = 30)
    private ReviewVisibility visibilityStatus;

    @Column(name = "IsReported")
    private Boolean isReported;

    @Column(name = "CreatedAt", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "UpdatedAt")
    private LocalDateTime updatedAt;

    @Column(name = "RepliedAt")
    private LocalDateTime repliedAt;
}
