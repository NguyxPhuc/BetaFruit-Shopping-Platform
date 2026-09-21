package com.fu.SWP391_BetaFruit.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "[Address]")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AddressId")
    private Integer addressId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserId", nullable = false)
    private User user;

    @Column(name = "ReceiverName", nullable = false, columnDefinition = "NVARCHAR(100)")
    private String receiverName;

    @Column(name = "ReceiverPhone", nullable = false, length = 20)
    private String receiverPhone;

    @Column(name = "AddressLine", nullable = false, columnDefinition = "NVARCHAR(255)")
    private String addressLine;

    @Column(name = "City", nullable = false, columnDefinition = "NVARCHAR(100)")
    private String city;

    @Column(name = "District", nullable = false, columnDefinition = "NVARCHAR(100)")
    private String district;

    @Column(name = "Ward", nullable = false, columnDefinition = "NVARCHAR(100)")
    private String ward;

    @Column(name = "IsDefault")
    private Boolean isDefault;

    @Column(name = "IsActive")
    private Boolean isActive;
}
