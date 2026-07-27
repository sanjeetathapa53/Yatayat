package com.yatayat.backend.controller;

import com.yatayat.backend.dto.AdminLiveMonitoringResponse;
import com.yatayat.backend.service.AdminLiveMonitoringService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/live-monitoring")
public class AdminLiveMonitoringController {
    private final AdminLiveMonitoringService service;

    public AdminLiveMonitoringController(AdminLiveMonitoringService service) {
        this.service = service;
    }

    @GetMapping
    public AdminLiveMonitoringResponse snapshot() {
        return service.snapshot();
    }
}
