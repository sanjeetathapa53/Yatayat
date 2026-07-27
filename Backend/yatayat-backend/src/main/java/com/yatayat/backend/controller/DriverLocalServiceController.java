package com.yatayat.backend.controller;

import com.yatayat.backend.dto.LocalServiceLocationResponse;
import com.yatayat.backend.dto.LocalServiceRunResponse;
import com.yatayat.backend.dto.TripLocationUpdateRequest;
import com.yatayat.backend.service.LocalServiceLocationService;
import com.yatayat.backend.service.LocalServiceRunService;
import jakarta.validation.Valid;
import org.springframework.web.bind.MethodArgumentNotValidException;
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
    private final LocalServiceLocationService locationService;

    public DriverLocalServiceController(
            LocalServiceRunService service,
            LocalServiceLocationService locationService
    ) {
        this.service = service;
        this.locationService = locationService;
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

    @PutMapping("/{id}/location")
    public LocalServiceLocationResponse updateLocation(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody TripLocationUpdateRequest request
    ) {
        return locationService.update(authentication.getName(), id, request);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleStatus(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode()).body(Map.of(
                "success", false,
                "message", exception.getReason() == null
                        ? "Local service request could not be completed." : exception.getReason()
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
