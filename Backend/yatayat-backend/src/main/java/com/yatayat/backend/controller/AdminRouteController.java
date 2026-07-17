package com.yatayat.backend.controller;

import com.yatayat.backend.dto.RouteRequest;
import com.yatayat.backend.dto.RouteResponse;
import com.yatayat.backend.service.RouteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/routes")
public class AdminRouteController {

    private final RouteService routeService;

    public AdminRouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    @GetMapping
    public List<RouteResponse> getRoutes() {
        return routeService.getAllRoutes();
    }

    @GetMapping("/{id}")
    public RouteResponse getRoute(@PathVariable Long id) {
        return routeService.getRoute(id);
    }

    @PostMapping
    public ResponseEntity<RouteResponse> createRoute(
            @RequestBody RouteRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(routeService.createRoute(request));
    }

    @PutMapping("/{id}")
    public RouteResponse updateRoute(
            @PathVariable Long id,
            @RequestBody RouteRequest request
    ) {
        return routeService.updateRoute(id, request);
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

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleMalformedRequest() {
        return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Request contains an invalid value or data type"
        ));
    }
}
