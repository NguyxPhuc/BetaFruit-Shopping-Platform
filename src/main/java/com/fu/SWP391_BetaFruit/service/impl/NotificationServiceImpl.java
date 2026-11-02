package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.dto.response.NotificationResponse;
import com.fu.SWP391_BetaFruit.entity.Notification;
import com.fu.SWP391_BetaFruit.entity.User;
import com.fu.SWP391_BetaFruit.enums.NotificationType;
import com.fu.SWP391_BetaFruit.repository.NotificationRepository;
import com.fu.SWP391_BetaFruit.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Override
    public void createBaseNotification(User user, String title, String message, NotificationType type, String actionUrl) {
        if (user == null) {
            throw new IllegalArgumentException("User nhận thông báo không được để trống!");
        }
        Notification notif = new Notification();
        notif.setUser(user);
        notif.setTitle(title);
        notif.setMessage(message);
        notif.setNotificationType(type);
        notif.setIsRead(false);
        notif.setIsHidden(false);
        notif.setCreatedAt(LocalDateTime.now());
        notif.setActionUrl(actionUrl);

        notificationRepository.save(notif);
    }

    @Override
    public int countUnreadNotifications(Integer userId) {
        if (userId == null) return 0;
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
    public List<NotificationResponse> getAllNotifications(Integer userId) {
        if (userId == null) return Collections.emptyList();
        List<Notification> notificationList = notificationRepository.findByUser_UserIdAndIsHiddenFalseOrderByCreatedAtDesc(userId);
        if (notificationList == null || notificationList.isEmpty()) {
            return Collections.emptyList();
        }
        List<NotificationResponse> responses = new ArrayList<>();
        for (Notification notification : notificationList) {
            responses.add(toNotificationResponse(notification));
        }
        return responses;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAllAsRead(Integer userId) {
        if (userId == null) return;
        List<Notification> unreadList = notificationRepository.findByUser_UserIdAndIsHiddenFalseAndIsReadFalse(userId);
        if (unreadList != null && !unreadList.isEmpty()) {
            for (Notification notification : unreadList) {
                notification.setIsRead(true);
            }
            notificationRepository.saveAll(unreadList);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAsRead(Integer userId, Integer notificationId) {
        if (userId == null || notificationId == null) {
            return;
        }
        Notification notification = notificationRepository.findByNotificationIdAndUser_UserIdAndIsHiddenFalse(notificationId, userId);
        if (notification != null && !Boolean.TRUE.equals(notification.getIsRead())) {
            notification.setIsRead(true);
            notificationRepository.save(notification);
        }
    }
}
