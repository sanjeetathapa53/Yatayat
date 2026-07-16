package com.yatayat.backend.controller;

import com.yatayat.backend.dto.*;
import com.yatayat.backend.service.DriverOperatorAssociationService;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/operator")
public class OperatorDriverController {

    private final DriverOperatorAssociationService associationService;

    public OperatorDriverController(DriverOperatorAssociationService associationService) {
        this.associationService = associationService;
    }

    @GetMapping("/drivers")
    public List<DriverOperatorAssociationResponse> getDrivers(Authentication authentication) {
        return associationService.getOperatorDrivers(authentication.getName());
    }

    @GetMapping("/drivers/eligible")
    public List<EligibleDriverResponse> getEligibleDrivers(
            Authentication authentication,
            @RequestParam(defaultValue = "") String query
    ) {
        return associationService.getEligibleDrivers(authentication.getName(), query);
    }

    @PostMapping("/driver-invitations")
    public ResponseEntity<DriverOperatorAssociationResponse> invite(
            Authentication authentication,
            @RequestBody DriverInvitationRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                associationService.invite(authentication.getName(), request));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handle(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode()).body(
                Map.of(
                        "success", false,
                        "message", exception.getReason() == null
                                ? "Request could not be completed"
                                : exception.getReason()
                ));
    }
}
