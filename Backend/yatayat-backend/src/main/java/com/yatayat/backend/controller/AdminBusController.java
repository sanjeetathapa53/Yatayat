package com.yatayat.backend.controller;

import com.yatayat.backend.dto.AdminBusResponse;
import com.yatayat.backend.dto.DriverRejectionRequest;
import com.yatayat.backend.service.AdminBusService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/buses")
public class AdminBusController {

    private final AdminBusService adminBusService;

    public AdminBusController(AdminBusService adminBusService) {
        this.adminBusService = adminBusService;
    }

    @GetMapping
    public ResponseEntity<List<AdminBusResponse>> getBuses(
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(adminBusService.getBuses(status));
    }

    @GetMapping("/{busId}")
    public ResponseEntity<AdminBusResponse> getBus(@PathVariable Long busId) {
        return ResponseEntity.ok(adminBusService.getBus(busId));
    }

    @PutMapping("/{busId}/approve")
    public ResponseEntity<AdminBusResponse> approve(@PathVariable Long busId) {
        return ResponseEntity.ok(adminBusService.approve(busId));
    }

    @PutMapping("/{busId}/reject")
    public ResponseEntity<AdminBusResponse> reject(
            @PathVariable Long busId,
            @RequestBody DriverRejectionRequest request
    ) {
        return ResponseEntity.ok(adminBusService.reject(busId, request.getReason()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleStatusException(
            ResponseStatusException exception
    ) {
        return ResponseEntity.status(exception.getStatusCode()).body(
                Map.of(
                        "success", false,
                        "message", exception.getReason() == null
                                ? "Request could not be completed"
                                : exception.getReason()
                )
        );
    }
}
