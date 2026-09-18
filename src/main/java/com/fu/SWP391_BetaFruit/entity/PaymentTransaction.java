package com.fu.SWP391_BetaFruit.entity;

import com.fu.SWP391_BetaFruit.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "PaymentTransaction",
        uniqueConstraints = @UniqueConstraint(columnNames = {"Gateway", "TransactionRef"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TransactionId")
    private Integer transactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OrderId", nullable = false)
    private Order order;

    @Column(name = "Gateway", nullable = false, length = 50)
    private String gateway;

    @Column(name = "TransactionRef", nullable = false, length = 100)
    private String transactionRef;

    @Column(name = "Amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", length = 20)
    private PaymentStatus status;

    @Column(name = "CreatedAt", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
