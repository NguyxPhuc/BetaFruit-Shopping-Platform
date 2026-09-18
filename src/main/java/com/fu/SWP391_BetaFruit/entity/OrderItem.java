package com.fu.SWP391_BetaFruit.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "OrderItem")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "OrderItemId")
    private Integer orderItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OrderId", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "VariantId", nullable = false)
    private ProductVariant variant;

    @Column(name = "Quantity", nullable = false)
    private Integer quantity;

    @Column(name = "PriceSnapshot", nullable = false, precision = 18, scale = 2)
    private BigDecimal priceSnapshot;

    @Column(name = "SnapshotProductName", nullable = false, columnDefinition = "NVARCHAR(255)")
    private String snapshotProductName;

    @Column(name = "SnapshotVariantName", nullable = false, columnDefinition = "NVARCHAR(100)")
    private String snapshotVariantName;
}
