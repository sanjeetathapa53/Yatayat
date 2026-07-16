package com.yatayat.backend.controller;

import com.yatayat.backend.service.DriverApplicationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.Authentication;
import com.yatayat.backend.entity.User;
import com.yatayat.backend.service.AuthenticatedUserService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/drivers")
public class DriverApplicationController {

    private final DriverApplicationService applicationService;
    private final AuthenticatedUserService authenticatedUserService;

    public DriverApplicationController(
            DriverApplicationService applicationService,
            AuthenticatedUserService authenticatedUserService
    ) {
        this.applicationService = applicationService;
        this.authenticatedUserService = authenticatedUserService;
    }

    @PostMapping(
            value = "/application",
            consumes = "multipart/form-data"
    )
    public ResponseEntity<Map<String, Object>> submitApplication(
            @RequestParam String email,
            @RequestParam String dateOfBirth,
            @RequestParam String permanentAddress,
            @RequestParam String currentAddress,
            @RequestParam String emergencyContactName,
            @RequestParam String emergencyContactPhone,
            @RequestParam String citizenshipNumber,
            @RequestParam String licenseNumber,
            @RequestParam String licenseCategory,
            @RequestParam String licenseIssueDate,
            @RequestParam String licenseExpiryDate,
            @RequestParam Integer yearsOfExperience,
            @RequestParam(required = false)
            String preferredOperatingArea,
            @RequestParam(required = false)
            String applicationNote,
            @RequestPart MultipartFile profilePhoto,
            @RequestPart MultipartFile citizenshipFront,
            @RequestPart MultipartFile citizenshipBack,
            @RequestPart MultipartFile licenseFront,
            @RequestPart MultipartFile licenseBack,
            Authentication authentication
    ) {
        try {
            User authenticatedUser = authenticatedUserService.requireUser(authentication);

            Map<String, Object> response =
                    applicationService.submitApplication(
                            authenticatedUser.getEmail(),
                            dateOfBirth,
                            permanentAddress,
                            currentAddress,
                            emergencyContactName,
                            emergencyContactPhone,
                            citizenshipNumber,
                            licenseNumber,
                            licenseCategory,
                            licenseIssueDate,
                            licenseExpiryDate,
                            yearsOfExperience,
                            preferredOperatingArea,
                            applicationNote,
                            profilePhoto,
                            citizenshipFront,
                            citizenshipBack,
                            licenseFront,
                            licenseBack
                    );

            return ResponseEntity.ok(response);

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
                            "Unable to submit driver application"
                    )
            );
        }
    }

    @GetMapping("/status/{userId}")
    public ResponseEntity<Map<String, Object>> getStatus(
            @PathVariable Long userId,
            Authentication authentication
    ) {
        try {
            authenticatedUserService.requireOwnedUser(authentication, userId);
            return ResponseEntity.ok(
                    applicationService.getApplicationStatus(userId)
            );

        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(
                    errorResponse(exception.getMessage())
            );
        }
    }

    @GetMapping("/profile/{userId}")
    public ResponseEntity<Map<String, Object>> getDriverProfile(
            @PathVariable Long userId,
            Authentication authentication
    ) {
        try {
            authenticatedUserService.requireOwnedUser(authentication, userId);
            return ResponseEntity.ok(
                    applicationService.getDriverProfile(userId)
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

