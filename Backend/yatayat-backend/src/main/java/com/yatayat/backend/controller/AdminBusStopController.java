package com.yatayat.backend.controller;

import com.yatayat.backend.dto.BusStopRequest;
import com.yatayat.backend.dto.BusStopResponse;
import com.yatayat.backend.service.BusStopService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/stops")
public class AdminBusStopController {
    private final BusStopService busStopService;

    public AdminBusStopController(BusStopService busStopService) {
        this.busStopService = busStopService;
    }

    @GetMapping
    public List<BusStopResponse> getStops() {
        return busStopService.getStops();
    }

    @GetMapping("/{id}")
    public BusStopResponse getStop(@PathVariable Long id) {
        return busStopService.getStop(id);
    }

    @PostMapping
    public ResponseEntity<BusStopResponse> createStop(@RequestBody BusStopRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(busStopService.createStop(request));
    }

    @PutMapping("/{id}")
    public BusStopResponse updateStop(@PathVariable Long id, @RequestBody BusStopRequest request) {
        return busStopService.updateStop(id, request);
    }

    @PatchMapping("/{id}/status")
    public BusStopResponse setStatus(@PathVariable Long id, @RequestBody Map<String, Boolean> request) {
        return busStopService.setActive(id, Boolean.TRUE.equals(request.get("active")));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleStatus(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode()).body(Map.of(
                "success", false,
                "message", exception.getReason() == null ? "Request could not be completed" : exception.getReason()
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
