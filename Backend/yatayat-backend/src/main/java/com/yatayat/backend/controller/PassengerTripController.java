package com.yatayat.backend.controller;

import com.yatayat.backend.dto.PassengerTripDetailsResponse;
import com.yatayat.backend.dto.PassengerTripSearchResponse;
import com.yatayat.backend.service.PassengerTripService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/passenger/trips")
public class PassengerTripController {
    private final PassengerTripService passengerTripService;

    public PassengerTripController(PassengerTripService passengerTripService) {
        this.passengerTripService = passengerTripService;
    }

    @GetMapping("/search")
    public List<PassengerTripSearchResponse> search(
            Authentication authentication,
            @RequestParam String origin,
            @RequestParam String destination,
            @RequestParam(required = false) LocalDate date
    ) {
        return passengerTripService.search(authentication.getName(), origin, destination, date);
    }

    @GetMapping("/{tripId}")
    public PassengerTripDetailsResponse details(
            Authentication authentication, @PathVariable Long tripId
    ) {
        return passengerTripService.details(authentication.getName(), tripId);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleStatus(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode()).body(Map.of(
                "success", false,
                "message", exception.getReason() == null
                        ? "Request could not be completed" : exception.getReason()
        ));
    }
}
