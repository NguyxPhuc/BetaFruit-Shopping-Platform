package com.fu.SWP391_BetaFruit.entity;

import com.fu.SWP391_BetaFruit.enums.InventoryTransactionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "InventoryTransaction")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InventoryTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TransactionId")
    private Integer transactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "VariantId", nullable = false)
    private ProductVariant variant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserId", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "TransactionType", nullable = false, length = 50)
    private InventoryTransactionType transactionType;

    @Column(name = "QuantityChanged", nullable = false)
    private Integer quantityChanged;

    @Column(name = "PreviousStock", nullable = false)
    private Integer previousStock;

    @Column(name = "NewStock", nullable = false)
    private Integer newStock;

    @Column(name = "CreatedAt", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
