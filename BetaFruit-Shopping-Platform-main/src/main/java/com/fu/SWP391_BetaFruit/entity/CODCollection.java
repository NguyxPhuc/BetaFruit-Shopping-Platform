package com.fu.SWP391_BetaFruit.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "CODCollection")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CODCollection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CollectionId")
    private Integer collectionId;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ShipperId", nullable = false)
    private Shipper shipper;

    @Column(name = "AmountCollected", nullable = false, precision = 18, scale = 2)
    private BigDecimal amountCollected;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CollectedBy", nullable = false)
    private User collectedBy;

    @Column(name = "CreatedAt", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
