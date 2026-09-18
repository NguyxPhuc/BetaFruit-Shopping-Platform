package com.fu.SWP391_BetaFruit.entity;

import com.fu.SWP391_BetaFruit.enums.NotificationType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "Notification")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "NotificationId")
    private Integer notificationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserId", nullable = false)
    private User user;

    @Column(name = "Title", nullable = false, columnDefinition = "NVARCHAR(255)")
    private String title;

    @Column(name = "Message", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "NotificationType", nullable = false, length = 50)
    private NotificationType notificationType;

    @Column(name = "ActionUrl", length = 255)
    private String actionUrl;

    @Column(name = "IsRead")
    private Boolean isRead;

    @Column(name = "IsHidden")
    private Boolean isHidden;

    @Column(name = "CreatedAt", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
