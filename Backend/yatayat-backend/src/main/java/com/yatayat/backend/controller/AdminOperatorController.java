package com.yatayat.backend.controller;

import com.yatayat.backend.dto.DriverRejectionRequest;
import com.yatayat.backend.service.OperatorApplicationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/operators")
@CrossOrigin(origins = "http://localhost:5173")
public class AdminOperatorController {

    private final OperatorApplicationService applicationService;

    public AdminOperatorController(
            OperatorApplicationService applicationService
    ) {
        this.applicationService = applicationService;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getApplications() {
        return ResponseEntity.ok(
                applicationService.getAllApplications()
        );
    }

    @PutMapping("/{operatorId}/approve")
    public ResponseEntity<Map<String, Object>> approveApplication(
            @PathVariable Long operatorId
    ) {
        try {
            return ResponseEntity.ok(
                    applicationService.approveApplication(operatorId)
            );
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(
                    error(exception.getMessage())
            );
        }
    }

    @PutMapping("/{operatorId}/reject")
    public ResponseEntity<Map<String, Object>> rejectApplication(
            @PathVariable Long operatorId,
            @RequestBody DriverRejectionRequest request
    ) {
        try {
            return ResponseEntity.ok(
                    applicationService.rejectApplication(
                            operatorId,
                            request.getReason()
                    )
            );
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(
                    error(exception.getMessage())
            );
        }
    }

    private Map<String, Object> error(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return response;
    }
}
