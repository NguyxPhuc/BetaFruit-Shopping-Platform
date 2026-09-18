package com.fu.SWP391_BetaFruit.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "CartItem",
        uniqueConstraints = @UniqueConstraint(columnNames = {"CartId", "VariantId"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CartItemId")
    private Integer cartItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CartId", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "VariantId", nullable = false)
    private ProductVariant variant;

    @Column(name = "Quantity", nullable = false)
    private Integer quantity;

    @Column(name = "CreatedAt", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "UpdatedAt", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
