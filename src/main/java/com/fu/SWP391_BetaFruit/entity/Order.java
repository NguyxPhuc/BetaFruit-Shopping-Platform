package com.fu.SWP391_BetaFruit.entity;

import com.fu.SWP391_BetaFruit.enums.OrderStatus;
import com.fu.SWP391_BetaFruit.enums.PaymentMethod;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "[Order]")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "OrderId")
    private Integer orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CustomerId", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ShopId", nullable = false)
    private Shop shop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CouponId")
    private Coupon coupon;

    @Column(name = "SnapshotShippingAddress", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String snapshotShippingAddress;

    @Column(name = "TotalAmount", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "DiscountAmount", precision = 18, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "ShippingFee", precision = 18, scale = 2)
    private BigDecimal shippingFee;

    @Column(name = "FinalAmount", nullable = false, precision = 18, scale = 2)
    private BigDecimal finalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "PaymentMethod", nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "OrderStatus", length = 30)
    private OrderStatus orderStatus;

    @Column(name = "IsSettled")
    private Boolean isSettled;

    @Column(name = "SettledAt")
    private LocalDateTime settledAt;

    @Column(name = "CreatedAt", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
