package com.yatayat.backend.controller;

import com.yatayat.backend.dto.BusStopResponse;
import com.yatayat.backend.service.BusStopService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/passenger/stops")
public class PassengerStopController {
    private final BusStopService busStopService;

    public PassengerStopController(BusStopService busStopService) {
        this.busStopService = busStopService;
    }

    @GetMapping
    public List<BusStopResponse> search(@RequestParam(required = false) String query) {
        return busStopService.searchActiveStops(query);
    }
}
