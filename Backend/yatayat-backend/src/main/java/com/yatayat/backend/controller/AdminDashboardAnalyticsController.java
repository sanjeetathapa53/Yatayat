package com.yatayat.backend.controller;

import com.yatayat.backend.dto.AdminDashboardAnalyticsResponse;
import com.yatayat.backend.service.AdminDashboardAnalyticsService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/analytics")
public class AdminDashboardAnalyticsController {
    private final AdminDashboardAnalyticsService analytics;

    public AdminDashboardAnalyticsController(AdminDashboardAnalyticsService analytics) {
        this.analytics = analytics;
    }

    @GetMapping("/dashboard")
    public AdminDashboardAnalyticsResponse dashboard(
            @RequestParam(defaultValue = "LAST_7_DAYS") String range) {
        try {
            return analytics.dashboard(range);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
        }
    }
}
