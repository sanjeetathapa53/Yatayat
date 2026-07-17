package com.yatayat.backend.controller;

import com.yatayat.backend.dto.*;
import com.yatayat.backend.entity.TripStatus;
import com.yatayat.backend.service.ScheduledTripService;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/operator/trips")
public class OperatorScheduledTripController {

    private final ScheduledTripService tripService;

    public OperatorScheduledTripController(ScheduledTripService tripService) {
        this.tripService = tripService;
    }

    @GetMapping("/eligibility")
    public TripEligibilityResponse eligibility(Authentication authentication) {
        return tripService.getEligibility(authentication.getName());
    }

    @PostMapping
    public ResponseEntity<TripResponse> create(
            Authentication authentication,
            @RequestBody TripCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(tripService.create(authentication.getName(), request));
    }

    @GetMapping
    public List<TripSummaryResponse> list(
            Authentication authentication,
            @RequestParam(required = false) TripStatus status,
            @RequestParam(required = false) LocalDateTime from,
            @RequestParam(required = false) LocalDateTime to
    ) {
        return tripService.list(authentication.getName(), status, from, to);
    }

    @GetMapping("/{tripId}")
    public TripResponse get(Authentication authentication, @PathVariable Long tripId) {
        return tripService.get(authentication.getName(), tripId);
    }

    @PutMapping("/{tripId}")
    public TripResponse update(
            Authentication authentication,
            @PathVariable Long tripId,
            @RequestBody TripUpdateRequest request
    ) {
        return tripService.update(authentication.getName(), tripId, request);
    }

    @PostMapping("/{tripId}/cancel")
    public TripResponse cancel(
            Authentication authentication,
            @PathVariable Long tripId,
            @RequestBody(required = false) TripCancellationRequest request
    ) {
        return tripService.cancel(authentication.getName(), tripId, request);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleStatus(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode()).body(Map.of(
                "success", false,
                "message", exception.getReason() == null
                        ? "Request could not be completed" : exception.getReason()
        ));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> malformed() {
        return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Request contains an invalid value or data type"
        ));
    }
}
