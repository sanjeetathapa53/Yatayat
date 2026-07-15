package com.yatayat.backend.controller;

import com.yatayat.backend.dto.DriverRejectionRequest;
import com.yatayat.backend.service.AdminDriverService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/drivers")
@CrossOrigin(origins = "http://localhost:5173")
public class AdminDriverController {

    private final AdminDriverService adminDriverService;

    public AdminDriverController(
            AdminDriverService adminDriverService
    ) {
        this.adminDriverService = adminDriverService;
    }

    @GetMapping("/pending")
    public ResponseEntity<List<Map<String, Object>>>
    getPendingApplications() {
        return ResponseEntity.ok(
                adminDriverService.getPendingApplications()
        );
    }

    @GetMapping("/{profileId}")
    public ResponseEntity<Map<String, Object>> getApplication(
            @PathVariable Long profileId
    ) {
        try {
            return ResponseEntity.ok(
                    adminDriverService.getApplication(profileId)
            );
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(
                    errorResponse(exception.getMessage())
            );
        }
    }

    @PutMapping("/{profileId}/approve")
    public ResponseEntity<Map<String, Object>> approveApplication(
            @PathVariable Long profileId
    ) {
        try {
            return ResponseEntity.ok(
                    adminDriverService.approveApplication(profileId)
            );
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(
                    errorResponse(exception.getMessage())
            );
        }
    }

    @PutMapping("/{profileId}/reject")
    public ResponseEntity<Map<String, Object>> rejectApplication(
            @PathVariable Long profileId,
            @RequestBody DriverRejectionRequest request
    ) {
        try {
            return ResponseEntity.ok(
                    adminDriverService.rejectApplication(
                            profileId,
                            request.getReason()
                    )
            );
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(
                    errorResponse(exception.getMessage())
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