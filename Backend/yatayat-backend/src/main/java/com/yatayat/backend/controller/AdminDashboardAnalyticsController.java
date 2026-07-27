package com.yatayat.backend.controller;

import com.yatayat.backend.dto.AdminDashboardAnalyticsResponse;
import com.yatayat.backend.service.AdminDashboardAnalyticsService;
import com.yatayat.backend.dto.AdminAnalyticsDetailsResponses.*;
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

    @GetMapping("/users")
    public UsersResponse users(@RequestParam(defaultValue = "LAST_7_DAYS") String range) {
        return validated(() -> analytics.users(range));
    }

    @GetMapping("/operations")
    public OperationsResponse operations(@RequestParam(defaultValue = "LAST_7_DAYS") String range) {
        return validated(() -> analytics.operations(range));
    }

    @GetMapping("/bookings")
    public BookingsResponse bookings(@RequestParam(defaultValue = "LAST_7_DAYS") String range) {
        return validated(() -> analytics.bookings(range));
    }

    @GetMapping("/revenue")
    public RevenueResponse revenue(@RequestParam(defaultValue = "LAST_7_DAYS") String range) {
        return validated(() -> analytics.revenue(range));
    }

    private <T> T validated(java.util.function.Supplier<T> action) {
        try {
            return action.get();
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
        }
    }
}
