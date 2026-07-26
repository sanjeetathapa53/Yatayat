package com.yatayat.backend.controller;

import com.yatayat.backend.dto.DriverTicketValidationRequest;
import com.yatayat.backend.dto.DriverTicketValidationResponse;
import com.yatayat.backend.dto.DriverTripManifestResponse;
import com.yatayat.backend.service.DriverTicketValidationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import jakarta.validation.Valid;

import java.util.Map;

@RestController
@RequestMapping("/api/driver")
public class DriverTicketController {
    private final DriverTicketValidationService validationService;

    public DriverTicketController(DriverTicketValidationService validationService) {
        this.validationService = validationService;
    }

    @PostMapping("/tickets/validate")
    public DriverTicketValidationResponse validate(Authentication authentication,
                                                   @Valid @RequestBody DriverTicketValidationRequest request) {
        return validationService.validate(authentication.getName(), request.qrPayload());
    }

    @GetMapping("/trips/{scheduledTripId}/manifest")
    public DriverTripManifestResponse manifest(Authentication authentication,
                                               @PathVariable Long scheduledTripId) {
        return validationService.manifest(authentication.getName(), scheduledTripId);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleStatus(ResponseStatusException exception) {
        String reason = exception.getReason() == null ? "Request could not be completed." : exception.getReason();
        String result = "ERROR";
        String message = reason;
        int separator = reason.indexOf('|');
        if (separator > 0) {
            result = reason.substring(0, separator);
            message = reason.substring(separator + 1);
        }
        return ResponseEntity.status(exception.getStatusCode()).body(Map.of(
                "result", result,
                "message", message,
                "success", false
        ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst().map(error -> error.getDefaultMessage())
                .orElse("QR payload is invalid.");
        return validationError(message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadable(
            HttpMessageNotReadableException exception) {
        return validationError("QR validation request is malformed.");
    }

    private ResponseEntity<Map<String, Object>> validationError(String message) {
        return ResponseEntity.badRequest().body(Map.of(
                "result", "INVALID_QR",
                "message", message,
                "success", false
        ));
    }
}
