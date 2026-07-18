package com.yatayat.backend.controller;

import com.yatayat.backend.dto.DriverTripOperationResponse;
import com.yatayat.backend.service.TripOperationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/driver/trips")
public class DriverTripOperationController {
    private final TripOperationService tripOperationService;

    public DriverTripOperationController(TripOperationService tripOperationService) {
        this.tripOperationService = tripOperationService;
    }

    @GetMapping("/current")
    public ResponseEntity<?> current(Authentication authentication) {
        return tripOperationService.currentDriverTrip(authentication.getName())
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
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

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleStatus(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode()).body(Map.of(
                "success", false,
                "message", exception.getReason() == null
                        ? "Trip operation could not be completed." : exception.getReason()
        ));
    }
}
