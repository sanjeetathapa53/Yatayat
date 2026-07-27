package com.yatayat.backend.controller;

import com.yatayat.backend.dto.OperatorLocalFleetLocationResponse;
import com.yatayat.backend.service.OperatorLocalLiveFleetService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/operator/local-live-fleet")
public class OperatorLocalLiveFleetController {
    private final OperatorLocalLiveFleetService fleetService;

    public OperatorLocalLiveFleetController(OperatorLocalLiveFleetService fleetService) {
        this.fleetService = fleetService;
    }

    @GetMapping
    public List<OperatorLocalFleetLocationResponse> activeFleet(Authentication authentication) {
        return fleetService.activeFleet(authentication.getName());
    }

    @GetMapping("/{runId}")
    public OperatorLocalFleetLocationResponse run(
            Authentication authentication,
            @PathVariable Long runId
    ) {
        return fleetService.run(authentication.getName(), runId);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleStatus(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode()).body(Map.of(
                "success", false,
                "message", exception.getReason() == null
                        ? "Local fleet tracking request could not be completed."
                        : exception.getReason()
        ));
    }
}
