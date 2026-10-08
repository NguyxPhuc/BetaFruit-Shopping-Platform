package com.fu.SWP391_BetaFruit.entity;

import com.fu.SWP391_BetaFruit.enums.ProductApprovalStatus;
import com.fu.SWP391_BetaFruit.enums.ProductVisibility;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "Product")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ProductId")
    private Integer productId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ShopId", nullable = false)
    private Shop shop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CategoryId", nullable = false)
    private MasterCategory category;

    @Column(name = "ProductName", nullable = false, columnDefinition = "NVARCHAR(255)")
    private String productName;

    @Column(name = "Description", columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "ApprovalStatus", length = 20)
    private ProductApprovalStatus approvalStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "VisibilityStatus", length = 20)
    private ProductVisibility visibilityStatus;

    @Column(name = "CreatedAt", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
