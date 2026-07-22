package com.yatayat.backend.controller;

import com.yatayat.backend.dto.*;
import com.yatayat.backend.service.PassengerSeatService;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

@RestController
@RequestMapping("/api/passenger/trips/{tripId}")
public class PassengerSeatController {
    private final PassengerSeatService seatService;
    public PassengerSeatController(PassengerSeatService seatService) { this.seatService = seatService; }
    @GetMapping({"/seats", "/availability"})
    public SeatAvailabilityResponse seats(Authentication auth, @PathVariable Long tripId) {
        return seatService.availability(auth.getName(), tripId);
    }
    @PostMapping("/seat-holds")
    public SeatHoldResponse hold(Authentication auth, @PathVariable Long tripId,
                                 @RequestBody SeatHoldRequest request) {
        return seatService.hold(auth.getName(), tripId, request);
    }
    @DeleteMapping("/seat-holds")
    public ResponseEntity<Void> release(Authentication auth, @PathVariable Long tripId) {
        seatService.release(auth.getName(), tripId); return ResponseEntity.noContent().build();
    }
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handle(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode()).body(Map.of("success", false,
                "message", exception.getReason() == null ? "Request could not be completed" : exception.getReason()));
    }
}
