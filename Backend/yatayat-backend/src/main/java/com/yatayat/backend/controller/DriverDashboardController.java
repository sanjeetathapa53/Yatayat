package com.yatayat.backend.controller;

import com.yatayat.backend.service.DriverDashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import com.yatayat.backend.service.AuthenticatedUserService;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/drivers/dashboard")
public class DriverDashboardController {

    private final DriverDashboardService dashboardService;
    private final AuthenticatedUserService authenticatedUserService;

    public DriverDashboardController(
            DriverDashboardService dashboardService,
            AuthenticatedUserService authenticatedUserService
    ) {
        this.dashboardService = dashboardService;
        this.authenticatedUserService = authenticatedUserService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> getDashboard(
            @PathVariable Long userId,
            Authentication authentication
    ) {
        try {
            authenticatedUserService.requireOwnedUser(authentication, userId);
            return ResponseEntity.ok(
                    dashboardService.getDashboard(userId)
            );
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(
                    errorResponse(exception.getMessage())
            );
        } catch (Exception exception) {
            exception.printStackTrace();

            return ResponseEntity.internalServerError().body(
                    errorResponse(
                            "Unable to load driver dashboard"
                    )
            );
        }
    }

    private Map<String, Object> errorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return response;
    }
}
