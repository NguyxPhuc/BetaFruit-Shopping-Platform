package com.fu.SWP391_BetaFruit.controller;

import com.fu.SWP391_BetaFruit.dto.response.NotificationResponse;
import com.fu.SWP391_BetaFruit.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @PostMapping("/read/{id}")
    public String markAsRead(@PathVariable("id") Integer notificationId,
                             @RequestParam(value = "redirect", required = false) String redirectUrl,
                             HttpSession session,
                             HttpServletRequest request) {
        Integer userId = (Integer) session.getAttribute("LOGGED_IN_USER_ID");
        if (userId == null) {
            return "redirect:/auth/login";
        }

        try {
            notificationService.markAsRead(userId, notificationId);
        } catch (Exception ignored) {
        }

        // Nếu thông báo có link điều hướng (actionUrl) thì chuyển tiếp
        if (redirectUrl != null && !redirectUrl.isBlank() && !redirectUrl.equals("#")) {
            return "redirect:" + redirectUrl;
        }

        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isBlank() && !referer.contains("/notifications")) {
            return "redirect:" + referer;
        }

        return "redirect:/";
    }

    @GetMapping("/read/{id}")
    public String markAsReadGetFallback(@PathVariable("id") Integer notificationId,
                                        @RequestParam(value = "redirect", required = false) String redirectUrl,
                                        HttpServletRequest request) {
        if (redirectUrl != null && !redirectUrl.isBlank() && !redirectUrl.equals("#")) {
            return "redirect:" + redirectUrl;
        }
        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isBlank() && !referer.contains("/notifications")) {
            return "redirect:" + referer;
        }
        return "redirect:/";
    }

    @PostMapping("/read-all")
    public String markAllAsRead(HttpSession session,
                                HttpServletRequest request) {
        Integer userId = (Integer) session.getAttribute("LOGGED_IN_USER_ID");
        if (userId == null) {
            return "redirect:/auth/login";
        }

        try {
            notificationService.markAllAsRead(userId);
        } catch (Exception ignored) {
        }

        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isBlank() && !referer.contains("/notifications")) {
            return "redirect:" + referer;
        }
        return "redirect:/";
    }


    @GetMapping("/read-all")
    public String markAllAsReadGetFallback(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isBlank() && !referer.contains("/notifications")) {
            return "redirect:" + referer;
        }
        return "redirect:/";
    }

    @GetMapping("")
    public String getNotificationsFallback(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isBlank() && !referer.contains("/notifications")) {
            return "redirect:" + referer;
        }
        return "redirect:/";
    }
}
