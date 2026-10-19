package com.fu.SWP391_BetaFruit.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "MembershipTier")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MembershipTier {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TierId")
    private Integer tierId;

    @Column(name = "TierName", nullable = false, columnDefinition = "NVARCHAR(100)")
    private String tierName;

    @Column(name = "MinSpentRequired", precision = 18, scale = 2)
    private BigDecimal minSpentRequired;

    @Column(name = "DiscountPercent", precision = 5, scale = 2)
    private BigDecimal discountPercent;
}
