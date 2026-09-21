package com.fu.SWP391_BetaFruit.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "CustomerMembership")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CustomerMembership {
    @Id
    @Column(name = "UserId")
    private Integer userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "UserId")
    private User user;

    @Column(name = "TotalCumulativeSpend", precision = 18, scale = 2)
    private BigDecimal totalCumulativeSpend;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TierId")
    private MembershipTier tier;
}
