package com.yatayat.backend.controller;

import com.yatayat.backend.dto.OperatorDashboardResponse;
import com.yatayat.backend.service.OperatorDashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/operator/dashboard")
@CrossOrigin(
        origins = "http://localhost:5173",
        allowCredentials = "true"
)
public class OperatorDashboardController {

    private final OperatorDashboardService dashboardService;

    public OperatorDashboardController(
            OperatorDashboardService dashboardService
    ) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public ResponseEntity<OperatorDashboardResponse> getDashboard(
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                dashboardService.getDashboard(authentication.getName())
        );
    }
}
