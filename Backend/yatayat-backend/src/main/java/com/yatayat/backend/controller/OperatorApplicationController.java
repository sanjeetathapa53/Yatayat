package com.yatayat.backend.controller;

import com.yatayat.backend.dto.OperatorApplicationRequest;
import com.yatayat.backend.service.OperatorApplicationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import com.yatayat.backend.service.AuthenticatedUserService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/operators")
public class OperatorApplicationController {

    private final OperatorApplicationService applicationService;
    private final AuthenticatedUserService authenticatedUserService;

    public OperatorApplicationController(
            OperatorApplicationService applicationService,
            AuthenticatedUserService authenticatedUserService
    ) {
        this.applicationService = applicationService;
        this.authenticatedUserService = authenticatedUserService;
    }

    @PostMapping("/application")
    public ResponseEntity<Map<String, Object>>
    submitApplication(
            @RequestBody OperatorApplicationRequest request,
            Authentication authentication
    ) {
        try {
            authenticatedUserService.requireOwnedUser(authentication, request.getUserId());
            return ResponseEntity.ok(
                    applicationService.submitApplication(request)
            );
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(
                    error(exception.getMessage())
            );
        }
    }

    @PutMapping("/application/resubmit")
    public ResponseEntity<Map<String, Object>>
    resubmitApplication(
            @RequestBody OperatorApplicationRequest request,
            Authentication authentication
    ) {
        try {
            authenticatedUserService.requireOwnedUser(authentication, request.getUserId());
            return ResponseEntity.ok(
                    applicationService.resubmitApplication(
                            request
                    )
            );
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(
                    error(exception.getMessage())
            );
        }
    }

    @GetMapping("/status/{userId}")
    public ResponseEntity<Map<String, Object>>
    getApplicationStatus(
            @PathVariable Long userId,
            Authentication authentication
    ) {
        try {
            authenticatedUserService.requireOwnedUser(authentication, userId);
            return ResponseEntity.ok(
                    applicationService
                            .getApplicationStatus(userId)
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
