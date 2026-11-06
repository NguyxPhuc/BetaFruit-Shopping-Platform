package com.fu.SWP391_BetaFruit.repository;

import com.fu.SWP391_BetaFruit.entity.Notification;
import com.fu.SWP391_BetaFruit.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {
    int countByUser_UserIdAndIsReadFalseAndIsHiddenFalse(Integer userId);

    List<Notification> findByUser_UserIdAndIsHiddenFalseOrderByCreatedAtDesc(Integer userId);

    List<Notification> findTop20ByUser_UserIdAndIsHiddenFalseOrderByCreatedAtDescNotificationIdDesc(Integer userId);

    boolean existsByUser_UserIdAndNotificationTypeAndActionUrlAndTitle(
            Integer userId, NotificationType type, String actionUrl, String title);

    List<Notification> findByUser_UserIdAndIsHiddenFalseAndIsReadFalse(Integer userId);

    Notification findByNotificationIdAndUser_UserIdAndIsHiddenFalse(Integer notificationId, Integer userId);

    @Query("SELECT COUNT(n) > 0 FROM Notification n " +
           "WHERE n.user.userId = :userId " +
           "  AND n.notificationType = :type " +
           "  AND n.title = :title " +
           "  AND n.createdAt >= :afterTime")
    boolean hasRecentNotification(
            @Param("userId") Integer userId,
            @Param("type") NotificationType type,
            @Param("title") String title,
            @Param("afterTime") LocalDateTime afterTime);
}
