package com.fu.SWP391_BetaFruit.controller;

import com.fu.SWP391_BetaFruit.dto.response.NotificationResponse;
import com.fu.SWP391_BetaFruit.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.net.URI;

@Controller
@RequestMapping("/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    public record Feed(int unreadCount, List<NotificationResponse> notifications) {}

    @GetMapping("/feed")
    @ResponseBody
    public ResponseEntity<Feed> feed(HttpSession session) {
        Integer userId = (Integer) session.getAttribute("LOGGED_IN_USER_ID");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).cacheControl(CacheControl.noStore()).build();
        }
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new Feed(
                notificationService.countUnreadNotifications(userId),
                notificationService.getRecentNotifications(userId)));
    }

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

        if (isLocalPath(redirectUrl)) {
            return "redirect:" + redirectUrl;
        }
        return refererRedirect(request);
    }

    @GetMapping("/read/{id}")
    public String markAsReadGetFallback(@PathVariable("id") Integer notificationId,
                                        @RequestParam(value = "redirect", required = false) String redirectUrl,
                                        HttpServletRequest request) {
        if (isLocalPath(redirectUrl)) {
            return "redirect:" + redirectUrl;
        }
        return refererRedirect(request);
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

        return refererRedirect(request);
    }


    @GetMapping("/read-all")
    public String markAllAsReadGetFallback(HttpServletRequest request) {
        return refererRedirect(request);
    }

    @GetMapping("")
    public String getNotificationsFallback(HttpServletRequest request) {
        return refererRedirect(request);
    }

    private String refererRedirect(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer != null) {
            try {
                URI uri = URI.create(referer);
                URI current = URI.create(request.getRequestURL().toString());
                if (java.util.Objects.equals(uri.getScheme(), current.getScheme())
                        && java.util.Objects.equals(uri.getRawAuthority(), current.getRawAuthority())
                        && isLocalPath(uri.getRawPath()) && !uri.getPath().contains("/notifications")) {
                    return "redirect:" + uri.getRawPath() + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery());
                }
            } catch (IllegalArgumentException ignored) {
                // Invalid referers fall back to the home page.
            }
        }
        return "redirect:/";
    }

    private boolean isLocalPath(String value) {
        if (value == null || !value.startsWith("/") || value.startsWith("//") || value.contains("\\")) return false;
        try {
            URI uri = URI.create(value);
            String decodedPath = uri.getPath();
            return uri.getRawAuthority() == null && decodedPath != null && !decodedPath.startsWith("//")
                    && !decodedPath.contains("\\") && value.chars().noneMatch(Character::isISOControl)
                    && decodedPath.chars().noneMatch(Character::isISOControl);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
