package com.fu.SWP391_BetaFruit.service;

import com.fu.SWP391_BetaFruit.dto.response.NotificationResponse;
import com.fu.SWP391_BetaFruit.entity.User;
import com.fu.SWP391_BetaFruit.enums.NotificationType;

import java.util.List;

public interface NotificationService {
    void createBaseNotification(User user, String title, String message, NotificationType type, String actionUrl);

    int countUnreadNotifications(Integer userId);

    List<NotificationResponse> getRecentNotifications(Integer userId) throws Exception;

    List<NotificationResponse> getAllNotifications(Integer userId) throws Exception;
}
