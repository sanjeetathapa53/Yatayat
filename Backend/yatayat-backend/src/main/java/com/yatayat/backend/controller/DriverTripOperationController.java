package com.yatayat.backend.controller;

import com.yatayat.backend.dto.DriverTripOperationResponse;
import com.yatayat.backend.dto.TripLocationResponse;
import com.yatayat.backend.dto.TripLocationUpdateRequest;
import com.yatayat.backend.service.TripOperationService;
import com.yatayat.backend.service.TripLocationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/driver/trips")
public class DriverTripOperationController {
    private final TripOperationService tripOperationService;
    private final TripLocationService tripLocationService;

    public DriverTripOperationController(
            TripOperationService tripOperationService,
            TripLocationService tripLocationService
    ) {
        this.tripOperationService = tripOperationService;
        this.tripLocationService = tripLocationService;
    }

    @GetMapping("/current")
    public ResponseEntity<?> current(Authentication authentication) {
        return tripOperationService.currentDriverTrip(authentication.getName())
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/{scheduledTripId}/boarding")
    public DriverTripOperationResponse beginBoarding(
            Authentication authentication,
            @PathVariable Long scheduledTripId
    ) {
        return tripOperationService.beginBoarding(authentication.getName(), scheduledTripId);
    }

    @PostMapping("/{scheduledTripId}/start")
    public DriverTripOperationResponse start(Authentication authentication,
                                             @PathVariable Long scheduledTripId) {
        return tripOperationService.start(authentication.getName(), scheduledTripId);
    }

    @PostMapping("/{scheduledTripId}/finish")
    public DriverTripOperationResponse finish(Authentication authentication,
                                              @PathVariable Long scheduledTripId) {
        return tripOperationService.finish(authentication.getName(), scheduledTripId);
    }

    @PutMapping("/{tripId}/location")
    public TripLocationResponse updateLocation(
            Authentication authentication,
            @PathVariable Long tripId,
            @Valid @RequestBody TripLocationUpdateRequest request
    ) {
        return tripLocationService.update(authentication.getName(), tripId, request);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleStatus(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode()).body(Map.of(
                "success", false,
                "message", exception.getReason() == null
                        ? "Trip operation could not be completed." : exception.getReason()
        ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException exception
    ) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage() == null
                        ? "Invalid location value." : error.getDefaultMessage())
                .orElse("Invalid location value.");
        return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", message
        ));
    }
}
