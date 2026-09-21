package com.fu.SWP391_BetaFruit.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "ProductVariant")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "VariantId")
    private Integer variantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ProductId", nullable = false)
    private Product product;

    @Column(name = "VariantName", nullable = false, columnDefinition = "NVARCHAR(100)")
    private String variantName;

    @Column(name = "OriginalPrice", nullable = false, precision = 18, scale = 2)
    private BigDecimal originalPrice;

    @Column(name = "SalePrice", precision = 18, scale = 2)
    private BigDecimal salePrice;

    @Column(name = "StockQuantity", nullable = false)
    private Integer stockQuantity;

    @Column(name = "LowStockThreshold")
    private Integer lowStockThreshold;

    @Column(name = "IsAvailable")
    private Boolean isAvailable;
}
