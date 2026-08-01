package com.yatayat.backend.controller;

import com.yatayat.backend.dto.*;
import com.yatayat.backend.service.DriverNotificationService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/driver/notifications")
public class DriverNotificationController {
    private final DriverNotificationService service;
    public DriverNotificationController(DriverNotificationService service) { this.service = service; }
    @GetMapping public List<DriverNotificationResponse> list(
            Authentication authentication,
            @RequestParam(defaultValue = "false") boolean unreadOnly) {
        return service.list(authentication.getName(), unreadOnly);
    }
    @GetMapping("/unread-count")
    public DriverNotificationUnreadCountResponse unreadCount(Authentication authentication) {
        return service.unreadCount(authentication.getName());
    }
    @PutMapping("/{notificationId}/read")
    public DriverNotificationResponse markRead(Authentication authentication,
                                                @PathVariable Long notificationId) {
        return service.markRead(authentication.getName(), notificationId);
    }
    @PutMapping("/read-all")
    public DriverNotificationUnreadCountResponse markAllRead(Authentication authentication) {
        return service.markAllRead(authentication.getName());
    }
}
