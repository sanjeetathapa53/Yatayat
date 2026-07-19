package com.yatayat.backend.controller;

import com.yatayat.backend.dto.*;
import com.yatayat.backend.entity.LocalServiceRunStatus;
import com.yatayat.backend.service.LocalServiceRunService;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/operator/local-services")
public class OperatorLocalServiceController {
    private final LocalServiceRunService service;

    public OperatorLocalServiceController(LocalServiceRunService service) {
        this.service = service;
    }

    @GetMapping("/options")
    public LocalServiceOptionsResponse options(Authentication authentication) {
        return service.options(authentication.getName());
    }

    @GetMapping("/options/buses")
    public List<BusEligibilityResponse> buses(Authentication authentication) {
        return service.eligibleBuses(authentication.getName());
    }

    @GetMapping("/options/drivers")
    public List<DriverEligibilityResponse> drivers(Authentication authentication) {
        return service.eligibleDrivers(authentication.getName());
    }

    @GetMapping
    public List<LocalServiceRunResponse> list(
            Authentication authentication,
            @RequestParam(required = false) LocalServiceRunStatus status,
            @RequestParam(required = false) LocalDate serviceDate
    ) {
        return service.listOperatorRuns(authentication.getName(), status, serviceDate);
    }

    @GetMapping("/{id}")
    public LocalServiceRunResponse get(Authentication authentication, @PathVariable Long id) {
        return service.getOperatorRun(authentication.getName(), id);
    }

    @PostMapping
    public ResponseEntity<LocalServiceRunResponse> create(
            Authentication authentication, @RequestBody LocalServiceRunRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.create(authentication.getName(), request));
    }

    @PutMapping("/{id}")
    public LocalServiceRunResponse update(
            Authentication authentication, @PathVariable Long id,
            @RequestBody LocalServiceRunRequest request
    ) {
        return service.update(authentication.getName(), id, request);
    }

    @PatchMapping("/{id}/cancel")
    public LocalServiceRunResponse cancel(
            Authentication authentication, @PathVariable Long id,
            @RequestBody(required = false) LocalServiceCancellationRequest request
    ) {
        return service.cancel(authentication.getName(), id, request);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleStatus(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode()).body(Map.of(
                "success", false,
                "message", exception.getReason() == null
                        ? "Local service request could not be completed." : exception.getReason()
        ));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> malformed() {
        return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Request contains an invalid value or data type."
        ));
    }
}
