package com.yatayat.backend.controller;

import com.yatayat.backend.dto.AdminFleetLocationResponse;
import com.yatayat.backend.service.AdminLiveFleetService;
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
@RequestMapping("/api/admin/live-fleet")
public class AdminLiveFleetController {

    private final AdminLiveFleetService fleetService;

    public AdminLiveFleetController(AdminLiveFleetService fleetService) {
        this.fleetService = fleetService;
    }

    @GetMapping
    public List<AdminFleetLocationResponse> activeFleet() {
        return fleetService.activeFleet();
    }

    @GetMapping("/{tripId}")
    public AdminFleetLocationResponse trip(@PathVariable Long tripId) {
        return fleetService.trip(tripId);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleStatus(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode()).body(Map.of(
                "success", false,
                "message", exception.getReason() == null
                        ? "Live monitoring request could not be completed."
                        : exception.getReason()
        ));
    }
}
