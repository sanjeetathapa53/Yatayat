package com.yatayat.backend.controller;

import com.yatayat.backend.dto.LocalServiceRunResponse;
import com.yatayat.backend.service.LocalServiceRunService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/driver/local-services")
public class DriverLocalServiceController {
    private final LocalServiceRunService service;

    public DriverLocalServiceController(LocalServiceRunService service) {
        this.service = service;
    }

    @GetMapping
    public List<LocalServiceRunResponse> list(Authentication authentication) {
        return service.listDriverRuns(authentication.getName());
    }

    @GetMapping("/{id}")
    public LocalServiceRunResponse get(Authentication authentication, @PathVariable Long id) {
        return service.getDriverRun(authentication.getName(), id);
    }

    @GetMapping("/current")
    public ResponseEntity<?> current(Authentication authentication) {
        return service.currentDriverRun(authentication.getName())
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/{id}/start")
    public LocalServiceRunResponse start(Authentication authentication, @PathVariable Long id) {
        return service.startDriverRun(authentication.getName(), id);
    }

    @PostMapping("/{id}/finish")
    public LocalServiceRunResponse finish(Authentication authentication, @PathVariable Long id) {
        return service.finishDriverRun(authentication.getName(), id);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleStatus(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode()).body(Map.of(
                "success", false,
                "message", exception.getReason() == null
                        ? "Local service request could not be completed." : exception.getReason()
        ));
    }
}
