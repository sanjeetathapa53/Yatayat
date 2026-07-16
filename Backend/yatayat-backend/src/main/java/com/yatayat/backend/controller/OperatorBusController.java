package com.yatayat.backend.controller;

import com.yatayat.backend.dto.OperatorBusRequest;
import com.yatayat.backend.dto.OperatorBusResponse;
import com.yatayat.backend.service.OperatorBusService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/operator/buses")
public class OperatorBusController {

    private final OperatorBusService busService;

    public OperatorBusController(OperatorBusService busService) {
        this.busService = busService;
    }

    @PostMapping
    public ResponseEntity<OperatorBusResponse> createBus(
            Authentication authentication,
            @RequestBody OperatorBusRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                busService.createBus(authentication.getName(), request)
        );
    }

    @GetMapping
    public ResponseEntity<List<OperatorBusResponse>> getBuses(
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                busService.getBuses(authentication.getName())
        );
    }

    @GetMapping("/{busId}")
    public ResponseEntity<OperatorBusResponse> getBus(
            Authentication authentication,
            @PathVariable Long busId
    ) {
        return ResponseEntity.ok(
                busService.getBus(authentication.getName(), busId)
        );
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleStatusException(
            ResponseStatusException exception
    ) {
        return ResponseEntity.status(exception.getStatusCode()).body(
                Map.of(
                        "success", false,
                        "message", exception.getReason() == null
                                ? "Request could not be completed"
                                : exception.getReason()
                )
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleMalformedRequest() {
        return ResponseEntity.badRequest().body(
                Map.of(
                        "success", false,
                        "message", "Request contains an invalid value or data type"
                )
        );
    }
}
