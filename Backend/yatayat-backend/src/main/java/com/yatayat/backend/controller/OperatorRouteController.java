package com.yatayat.backend.controller;

import com.yatayat.backend.dto.RouteResponse;
import com.yatayat.backend.service.RouteService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/operator/routes")
public class OperatorRouteController {

    private final RouteService routeService;

    public OperatorRouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    @GetMapping
    public List<RouteResponse> getActiveRoutes(Authentication authentication) {
        return routeService.getActiveRoutesForOperator(authentication.getName());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleStatus(
            ResponseStatusException exception
    ) {
        return ResponseEntity.status(exception.getStatusCode()).body(Map.of(
                "success", false,
                "message", exception.getReason() == null
                        ? "Request could not be completed"
                        : exception.getReason()
        ));
    }
}
