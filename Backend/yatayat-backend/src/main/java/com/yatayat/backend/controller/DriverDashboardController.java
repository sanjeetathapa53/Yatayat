package com.yatayat.backend.controller;

import com.yatayat.backend.service.DriverDashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/drivers/dashboard")
@CrossOrigin(origins = "http://localhost:5173")
public class DriverDashboardController {

    private final DriverDashboardService dashboardService;

    public DriverDashboardController(
            DriverDashboardService dashboardService
    ) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> getDashboard(
            @PathVariable Long userId
    ) {
        try {
            return ResponseEntity.ok(
                    dashboardService.getDashboard(userId)
            );
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