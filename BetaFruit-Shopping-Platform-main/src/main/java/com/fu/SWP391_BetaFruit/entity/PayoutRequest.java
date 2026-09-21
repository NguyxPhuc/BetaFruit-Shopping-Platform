package com.fu.SWP391_BetaFruit.entity;
import com.fu.SWP391_BetaFruit.enums.PayoutStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "PayoutRequest")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PayoutRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RequestId")
    private Integer requestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ShopId", nullable = false)
    private Shop shop;

    @Column(name = "RequestedAmount", nullable = false, precision = 18, scale = 2)
    private BigDecimal requestedAmount;

    @Column(name = "BankName", nullable = false, columnDefinition = "NVARCHAR(100)")
    private String bankName;

    @Column(name = "BankAccountNumber", nullable = false, length = 50)
    private String bankAccountNumber;

    @Column(name = "AccountName", nullable = false, columnDefinition = "NVARCHAR(100)")
    private String accountName;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", length = 20)
    private PayoutStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ProcessedBy")
    private User processedBy;

    @Column(name = "CreatedAt", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "UpdatedAt")
    private LocalDateTime updatedAt;
}
