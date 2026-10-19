package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.dto.response.NotificationResponse;
import com.fu.SWP391_BetaFruit.entity.Notification;
import com.fu.SWP391_BetaFruit.entity.User;
import com.fu.SWP391_BetaFruit.enums.NotificationType;
import com.fu.SWP391_BetaFruit.repository.NotificationRepository;
import com.fu.SWP391_BetaFruit.service.NotificationService;
import com.fu.SWP391_BetaFruit.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserService userService;

    @Override
    public void createBaseNotification(User user, String title, String message, NotificationType type, String actionUrl) {
        if (user == null) {
            throw new IllegalArgumentException("User nhận thông báo không được để trống!");
        }
        Notification notif = new Notification();
        notif.setUser(user);
        notif.setTitle(title);
        notif.setMessage(message);
        notif.setNotificationType(type); // VD: "ORDER", "SYSTEM", "PROMOTION"
        notif.setIsRead(false);
        notif.setIsHidden(false);
        notif.setCreatedAt(LocalDateTime.now());
        notif.setActionUrl(actionUrl);

        notificationRepository.save(notif);
    }

    @Override
    public int countUnreadNotifications(Integer userId) {
        return notificationRepository.countByUser_UserIdAndIsReadFalseAndIsHiddenFalse(userId);
    }

    private NotificationResponse toNotificationResponse(Notification notification) {
        NotificationResponse response = new NotificationResponse();
        response.setNotificationId(notification.getNotificationId());
        response.setMessage(notification.getMessage());
        response.setCreatedAt(notification.getCreatedAt());
        response.setIsRead(notification.getIsRead());
        response.setTitle(notification.getTitle());
        response.setActionUrl(notification.getActionUrl());
        return response;
    }

    @Override
    public List<NotificationResponse> getRecentNotifications(Integer userId) throws Exception {
        List<Notification> notificationList = notificationRepository.findTop5ByUser_UserIdAndIsHiddenFalseOrderByCreatedAtDesc(userId);
        if(notificationList.isEmpty() || notificationList == null) {
            throw new Exception("Chưa có thông báo nào.");
        }
        List<NotificationResponse> responses = new ArrayList<>();
        NotificationResponse response = new NotificationResponse();
        for (Notification notification : notificationList) {
            response = toNotificationResponse(notification);
            responses.add(response);
        }
        return responses;
    }

    @Override
    public List<NotificationResponse> getAllNotifications(Integer userId) throws Exception {
        List<NotificationResponse> responses = new ArrayList<>();
        List<Notification> notificationList = notificationRepository.findByUser_UserIdAndIsHiddenFalseOrderByCreatedAtDesc(userId);
        if(notificationList.isEmpty() || notificationList == null) {
            throw new Exception("Chưa có thông báo nào.");
        }
        NotificationResponse response = new NotificationResponse();
        for (Notification notification : notificationList) {
            response = toNotificationResponse(notification);
            responses.add(response);
        }
        return responses;
    }
}
