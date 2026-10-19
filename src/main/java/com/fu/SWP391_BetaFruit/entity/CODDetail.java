package com.fu.SWP391_BetaFruit.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;

@Entity
@Table(name = "CODDetail")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CODDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DetailId")
    private Integer detailId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CollectionId", nullable = false)
    private CODCollection codCollection;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OrderId", nullable = false, unique = true)
    private Order order;

    @Column(name = "Amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;
}