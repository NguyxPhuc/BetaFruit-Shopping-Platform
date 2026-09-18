package com.fu.SWP391_BetaFruit.entity;

import com.fu.SWP391_BetaFruit.enums.ShopApprovalStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Shop")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Shop {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ShopId")
    private Integer shopId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OwnerId", referencedColumnName = "UserId", nullable = false, unique = true)
    private User owner;

    @Column(name = "ShopName", nullable = false, columnDefinition = "NVARCHAR(255)")
    private String shopName;

    @Column(name = "ShopDescription", columnDefinition = "NVARCHAR(MAX)")
    private String shopDescription;

    @Column(name = "LogoUrl", columnDefinition = "NVARCHAR(MAX)")
    private String logoUrl;

    @Column(name = "BannerUrl", columnDefinition = "NVARCHAR(MAX)")
    private String bannerUrl;

    @Column(name = "BankAccountInfo", columnDefinition = "NVARCHAR(255)")
    private String bankAccountInfo;

    @Column(name = "AvailableBalance", precision = 18, scale = 2)
    private BigDecimal availableBalance;

    @Enumerated(EnumType.STRING)
    @Column(name = "ApprovalStatus", length = 20)
    private ShopApprovalStatus approvalStatus;

    @Column(name = "CreatedAt", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
