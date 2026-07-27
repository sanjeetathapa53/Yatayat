package com.yatayat.backend.controller;

import com.yatayat.backend.dto.PassengerLocalRouteResponse;
import com.yatayat.backend.service.PassengerLocalRouteService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/passenger/local-routes")
public class PassengerLocalRouteController {
    private final PassengerLocalRouteService routeService;

    public PassengerLocalRouteController(PassengerLocalRouteService routeService) {
        this.routeService = routeService;
    }

    @GetMapping
    public List<PassengerLocalRouteResponse> list(Authentication authentication) {
        return routeService.list(authentication.getName());
    }

    @GetMapping("/search")
    public List<PassengerLocalRouteResponse> search(Authentication authentication,
                                                     @RequestParam(required = false) String origin,
                                                     @RequestParam(required = false) String destination,
                                                     @RequestParam(required = false) Long fromStopId,
                                                     @RequestParam(required = false) Long toStopId) {
        if (fromStopId != null || toStopId != null) {
            return routeService.searchByStopIds(authentication.getName(), fromStopId, toStopId);
        }
        return routeService.search(authentication.getName(), origin, destination);
    }

    @GetMapping("/{routeId}")
    public PassengerLocalRouteResponse details(Authentication authentication, @PathVariable Long routeId) {
        return routeService.details(authentication.getName(), routeId);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleStatus(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode()).body(Map.of(
                "success", false,
                "message", exception.getReason() == null ? "Request could not be completed" : exception.getReason()
        ));
    }
}
