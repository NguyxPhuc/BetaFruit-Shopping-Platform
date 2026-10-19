package com.fu.SWP391_BetaFruit.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ImportNote")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ImportNote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ImportId")
    private Integer importId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ShopId", nullable = false)
    private Shop shop;

    @Column(name = "TotalCost", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalCost;

    @Column(name = "Note", columnDefinition = "NVARCHAR(255)")
    private String note;

    @Column(name = "CreatedAt", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
