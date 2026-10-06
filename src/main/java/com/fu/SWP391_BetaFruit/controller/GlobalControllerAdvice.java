package com.fu.SWP391_BetaFruit.controller;

import com.fu.SWP391_BetaFruit.dto.response.NotificationResponse;
import com.fu.SWP391_BetaFruit.service.NotificationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

@ControllerAdvice
public class GlobalControllerAdvice {
    @Autowired
    private NotificationService notificationService;

    @ModelAttribute
    public void addGlobalAttributes(HttpSession session, Model model) {
        Integer userId = (Integer) session.getAttribute("LOGGED_IN_USER_ID");

        if (userId != null) {
            try {
                int unreadCount = notificationService.countUnreadNotifications(userId);
                model.addAttribute("globalUnreadCount", unreadCount);

                List<NotificationResponse> recentNotifications = notificationService.getRecentNotifications(userId);
                model.addAttribute("globalRecentNotifs", recentNotifications);

            } catch (Exception e) {
                model.addAttribute("globalUnreadCount", 0);
                model.addAttribute("globalRecentNotifs", null);
            }
        }
    }
}
