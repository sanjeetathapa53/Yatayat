package com.yatayat.backend.controller;

import com.yatayat.backend.dto.*;
import com.yatayat.backend.service.LocalFarePassService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/passenger/local-fare-passes")
public class PassengerLocalFarePassController {
    private final LocalFarePassService service;

    public PassengerLocalFarePassController(LocalFarePassService service) {
        this.service = service;
    }

    @PostMapping("/quote")
    public LocalFareQuoteResponse quote(
            Authentication authentication,
            @Valid @RequestBody LocalFareQuoteRequest request) {
        return service.quote(authentication.getName(), request);
    }

    @PostMapping
    public LocalFarePassResponse purchase(
            Authentication authentication,
            @Valid @RequestBody LocalFarePassPurchaseRequest request) {
        return service.purchase(authentication.getName(), request);
    }

    @GetMapping
    public List<LocalFarePassResponse> list(Authentication authentication) {
        return service.list(authentication.getName());
    }

    @GetMapping("/{passNumber}")
    public LocalFarePassResponse details(
            Authentication authentication,
            @PathVariable String passNumber) {
        return service.details(authentication.getName(), passNumber);
    }
}
