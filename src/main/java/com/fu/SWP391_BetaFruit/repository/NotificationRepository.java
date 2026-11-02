package com.fu.SWP391_BetaFruit.repository;

import com.fu.SWP391_BetaFruit.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {
    int countByUser_UserIdAndIsReadFalseAndIsHiddenFalse(Integer userId);

    List<Notification> findByUser_UserIdAndIsHiddenFalseOrderByCreatedAtDesc(Integer userId);

    List<Notification> findByUser_UserIdAndIsHiddenFalseAndIsReadFalse(Integer userId);

    Notification findByNotificationIdAndUser_UserIdAndIsHiddenFalse(Integer notificationId, Integer userId);
}
