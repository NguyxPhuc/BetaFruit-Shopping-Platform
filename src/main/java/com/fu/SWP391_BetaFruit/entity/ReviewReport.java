package com.fu.SWP391_BetaFruit.entity;

import com.fu.SWP391_BetaFruit.enums.DisputeStatus;
import com.fu.SWP391_BetaFruit.enums.ReportReason;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "ReviewReport")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ReportId")
    private Integer reportId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ReviewId", nullable = false)
    private Review review;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ShopId", nullable = false)
    private Shop shop;

    @Enumerated(EnumType.STRING)
    @Column(name = "ReasonCategory", nullable = false, length = 50)
    private ReportReason reasonCategory;

    @Column(name = "Explanation", nullable = false, columnDefinition = "NVARCHAR(1000)")
    private String explanation;

    @Column(name = "EvidenceUrls", columnDefinition = "NVARCHAR(MAX)")
    private String evidenceUrls;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", nullable = false, length = 30)
    private DisputeStatus status;

    @Column(name = "StaffNote", columnDefinition = "NVARCHAR(500)")
    private String staffNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ProcessedBy")
    private User processedBy;

    @Column(name = "CreatedAt")
    private LocalDateTime createdAt;

    @Column(name = "ResolvedAt")
    private LocalDateTime resolvedAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = DisputeStatus.PENDING;
        }
    }
}
