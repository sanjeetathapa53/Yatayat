package com.yatayat.backend.controller;

import com.yatayat.backend.dto.OperatorFleetLocationResponse;
import com.yatayat.backend.service.OperatorLiveFleetService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/operator/live-fleet")
public class OperatorLiveFleetController {

    private final OperatorLiveFleetService fleetService;

    public OperatorLiveFleetController(OperatorLiveFleetService fleetService) {
        this.fleetService = fleetService;
    }

    @GetMapping
    public List<OperatorFleetLocationResponse> activeFleet(Authentication authentication) {
        return fleetService.activeFleet(authentication.getName());
    }

    @GetMapping("/{tripId}")
    public OperatorFleetLocationResponse trip(
            Authentication authentication,
            @PathVariable Long tripId
    ) {
        return fleetService.trip(authentication.getName(), tripId);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleStatus(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode()).body(Map.of(
                "success", false,
                "message", exception.getReason() == null
                        ? "Fleet tracking request could not be completed."
                        : exception.getReason()
        ));
    }
}
