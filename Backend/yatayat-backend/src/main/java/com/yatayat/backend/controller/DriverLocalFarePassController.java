package com.yatayat.backend.controller;

import com.yatayat.backend.dto.DriverLocalFarePassValidationRequest;
import com.yatayat.backend.dto.DriverLocalFarePassValidationResponse;
import com.yatayat.backend.service.DriverLocalFarePassValidationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/driver/local-fare-passes")
public class DriverLocalFarePassController {
    private final DriverLocalFarePassValidationService service;

    public DriverLocalFarePassController(DriverLocalFarePassValidationService service) {
        this.service = service;
    }

    @PostMapping("/validate")
    public DriverLocalFarePassValidationResponse validate(
            Authentication authentication,
            @Valid @RequestBody DriverLocalFarePassValidationRequest request) {
        return service.validate(authentication.getName(), request.qrPayload());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleStatus(ResponseStatusException exception) {
        String reason = exception.getReason() == null ? "Request could not be completed." : exception.getReason();
        int separator = reason.indexOf('|');
        String result = separator > 0 ? reason.substring(0, separator) : "ERROR";
        String message = separator > 0 ? reason.substring(separator + 1) : reason;
        return ResponseEntity.status(exception.getStatusCode()).body(Map.of(
                "result", result, "message", message, "success", false));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst().map(error -> error.getDefaultMessage())
                .orElse("QR payload is invalid.");
        return ResponseEntity.badRequest().body(Map.of(
                "result", "INVALID_QR", "message", message, "success", false));
    }
}
