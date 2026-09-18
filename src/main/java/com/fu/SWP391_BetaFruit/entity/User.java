package com.fu.SWP391_BetaFruit.entity;

import com.fu.SWP391_BetaFruit.enums.UserStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "[User]") 
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "UserId")
    private Integer userId;

    @Column(name = "Username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "PasswordHash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "Email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "FullName", nullable = false, columnDefinition = "NVARCHAR(100)")
    private String fullName;

    @Column(name = "Phone", length = 20)
    private String phone;

    @Column(name = "AvatarUrl", columnDefinition = "NVARCHAR(MAX)")
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", length = 20)
    private UserStatus status;

    @Column(name = "CreatedAt", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    // Ánh xạ bảng trung gian UserRole
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "UserRole",
            joinColumns = @JoinColumn(name = "UserId"),
            inverseJoinColumns = @JoinColumn(name = "RoleId")
    )
    private List<Role> roles;
}
