package com.yatayat.backend.controller;

import com.yatayat.backend.dto.PassengerTripLocationResponse;
import com.yatayat.backend.service.PassengerLiveTrackingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/passenger/live-trips")
public class PassengerLiveTrackingController {

    private final PassengerLiveTrackingService trackingService;

    public PassengerLiveTrackingController(PassengerLiveTrackingService trackingService) {
        this.trackingService = trackingService;
    }

    @GetMapping
    public List<PassengerTripLocationResponse> activeLocations() {
        return trackingService.activeLocations();
    }

    @GetMapping("/{tripId}")
    public PassengerTripLocationResponse locationForTrip(@PathVariable Long tripId) {
        return trackingService.locationForTrip(tripId);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleStatus(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode()).body(Map.of(
                "success", false,
                "message", exception.getReason() == null ? "Tracking request could not be completed." : exception.getReason()
        ));
    }
}
