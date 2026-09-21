package com.fu.SWP391_BetaFruit.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "MasterCategory")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MasterCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CategoryId")
    private Integer categoryId;

    @Column(name = "CategoryName", nullable = false, columnDefinition = "NVARCHAR(255)")
    private String categoryName;

    @Column(name = "IsActive")
    private Boolean isActive;
}
