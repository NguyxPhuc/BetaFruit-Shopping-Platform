package com.fu.SWP391_BetaFruit.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "DeliveryAssignment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AssignmentId")
    private Integer assignmentId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OrderId", nullable = false, unique = true)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ShipperId", nullable = false)
    private Shipper shipper;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AssignedBy", nullable = false)
    private User assignedBy;

    @Column(name = "AssignedAt", insertable = false, updatable = false)
    private LocalDateTime assignedAt;
}
