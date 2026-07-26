package com.yatayat.backend.controller;

import com.yatayat.backend.dto.NotificationResponse;
import com.yatayat.backend.dto.NotificationUnreadCountResponse;
import com.yatayat.backend.service.NotificationService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/passenger/notifications")
public class PassengerNotificationController {
    private final NotificationService notifications;

    public PassengerNotificationController(NotificationService notifications) {
        this.notifications = notifications;
    }

    @GetMapping
    public List<NotificationResponse> list(Authentication authentication) {
        return notifications.list(authentication.getName());
    }

    @GetMapping("/unread-count")
    public NotificationUnreadCountResponse unreadCount(Authentication authentication) {
        return notifications.unreadCount(authentication.getName());
    }

    @PutMapping("/{notificationId}/read")
    public NotificationResponse markRead(Authentication authentication,
                                         @PathVariable Long notificationId) {
        return notifications.markRead(authentication.getName(), notificationId);
    }

    @PutMapping("/read-all")
    public NotificationUnreadCountResponse markAllRead(Authentication authentication) {
        return notifications.markAllRead(authentication.getName());
    }
}
