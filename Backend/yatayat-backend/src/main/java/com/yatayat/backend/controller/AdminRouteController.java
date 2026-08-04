package com.yatayat.backend.controller;

import com.yatayat.backend.dto.RouteRequest;
import com.yatayat.backend.dto.RouteResponse;
import com.yatayat.backend.dto.RouteStopRequest;
import com.yatayat.backend.entity.RouteStatus;
import com.yatayat.backend.entity.TripType;
import com.yatayat.backend.service.RouteDeletionService;
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
    private final RouteDeletionService routeDeletionService;

    public AdminRouteController(RouteService routeService, RouteDeletionService routeDeletionService) {
        this.routeService = routeService;
        this.routeDeletionService = routeDeletionService;
    }

    @GetMapping
    public List<RouteResponse> getRoutes(
            @RequestParam(required = false) TripType type,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String search
    ) {
        return routeService.getRoutes(type, active, search);
    }

    @GetMapping("/local")
    public List<RouteResponse> getLocalRoutes() {
        return routeService.getLocalRoutes();
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

    @PostMapping("/local")
    public ResponseEntity<RouteResponse> createLocalRoute(
            @RequestBody RouteRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(routeService.createLocalRoute(request));
    }

    @PutMapping("/{id}")
    public RouteResponse updateRoute(
            @PathVariable Long id,
            @RequestBody RouteRequest request
    ) {
        return routeService.updateRoute(id, request);
    }

    @PutMapping("/{id}/stops")
    public RouteResponse replaceStops(
            @PathVariable Long id,
            @RequestBody List<RouteStopRequest> stops
    ) {
        return routeService.replaceRouteStops(id, stops);
    }

    @PatchMapping("/{id}/status")
    public RouteResponse setStatus(
            @PathVariable Long id,
            @RequestBody Map<String, RouteStatus> request
    ) {
        return routeService.setStatus(id, request.get("status"));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRoute(@PathVariable Long id) {
        routeDeletionService.deleteRoute(id);
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
