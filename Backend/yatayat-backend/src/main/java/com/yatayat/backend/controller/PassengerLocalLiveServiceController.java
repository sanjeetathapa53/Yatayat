package com.yatayat.backend.controller;

import com.yatayat.backend.dto.PassengerLocalLiveServiceResponse;
import com.yatayat.backend.service.PassengerLocalLiveServiceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/passenger/local-live-services")
public class PassengerLocalLiveServiceController {
    private final PassengerLocalLiveServiceService service;

    public PassengerLocalLiveServiceController(PassengerLocalLiveServiceService service) {
        this.service = service;
    }

    @GetMapping
    public List<PassengerLocalLiveServiceResponse> activeServices(
            @RequestParam(required = false) Long routeId
    ) {
        return service.activeServices(routeId);
    }

    @GetMapping("/{id}")
    public PassengerLocalLiveServiceResponse activeService(@PathVariable Long id) {
        return service.activeService(id);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleStatus(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode()).body(Map.of(
                "success", false,
                "message", exception.getReason() == null
                        ? "Local tracking request could not be completed."
                        : exception.getReason()
        ));
    }
}
