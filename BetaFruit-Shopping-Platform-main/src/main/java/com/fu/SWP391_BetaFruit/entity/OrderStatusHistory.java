package com.fu.SWP391_BetaFruit.entity;

import com.fu.SWP391_BetaFruit.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "OrderStatusHistory")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "HistoryId")
    private Integer historyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OrderId", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UpdatedBy", nullable = false)
    private User updatedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "StatusFrom", length = 30)
    private OrderStatus statusFrom;

    @Enumerated(EnumType.STRING)
    @Column(name = "StatusTo", nullable = false, length = 30)
    private OrderStatus statusTo;

    @Column(name = "Note", columnDefinition = "NVARCHAR(255)")
    private String note;

    @Column(name = "CreatedAt", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
