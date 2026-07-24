package com.yatayat.backend.controller;

import com.yatayat.backend.dto.*;
import com.yatayat.backend.service.WalletTopUpService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/wallet/topups")
public class WalletTopUpController {
    private final WalletTopUpService service;

    public WalletTopUpController(WalletTopUpService service) {
        this.service = service;
    }

    @PostMapping("/khalti/initiate")
    public WalletTopUpInitiationResponse initiateKhalti(
            Authentication authentication, @RequestBody(required = false) CreateWalletTopUpRequest request) {
        return service.initiateKhalti(authentication.getName(), request);
    }

    @PostMapping("/esewa/initiate")
    public WalletTopUpInitiationResponse initiateEsewa(
            Authentication authentication, @RequestBody(required = false) CreateWalletTopUpRequest request) {
        return service.initiateEsewa(authentication.getName(), request);
    }

    @PostMapping("/{reference}/khalti/verify")
    public WalletTopUpVerificationResponse verifyKhalti(
            Authentication authentication, @PathVariable String reference,
            @RequestBody(required = false) KhaltiPaymentVerificationRequest request) {
        return service.verifyKhalti(authentication.getName(), reference, request);
    }

    @PostMapping("/{reference}/esewa/verify")
    public WalletTopUpVerificationResponse verifyEsewa(
            Authentication authentication, @PathVariable String reference,
            @RequestBody(required = false) EsewaPaymentVerificationRequest request) {
        return service.verifyEsewa(authentication.getName(), reference, request);
    }

    @GetMapping("/{reference}")
    public WalletTopUpVerificationResponse details(
            Authentication authentication, @PathVariable String reference) {
        return service.details(authentication.getName(), reference);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleStatus(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode()).body(Map.of(
                "success", false,
                "message", exception.getReason() == null
                        ? "Wallet top-up could not be completed." : exception.getReason()));
    }
}
