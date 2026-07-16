package com.yatayat.backend.controller;

import com.yatayat.backend.dto.DriverOperatorAssociationResponse;
import com.yatayat.backend.service.DriverOperatorAssociationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/driver")
public class DriverOperatorInvitationController {

    private final DriverOperatorAssociationService associationService;

    public DriverOperatorInvitationController(DriverOperatorAssociationService associationService) {
        this.associationService = associationService;
    }

    @GetMapping("/operator-invitations")
    public List<DriverOperatorAssociationResponse> invitations(Authentication authentication) {
        return associationService.getDriverInvitations(authentication.getName());
    }

    @PostMapping("/operator-invitations/{associationId}/accept")
    public DriverOperatorAssociationResponse accept(
            Authentication authentication,
            @PathVariable Long associationId
    ) {
        return associationService.accept(authentication.getName(), associationId);
    }

    @PostMapping("/operator-invitations/{associationId}/reject")
    public DriverOperatorAssociationResponse reject(
            Authentication authentication,
            @PathVariable Long associationId
    ) {
        return associationService.reject(authentication.getName(), associationId);
    }

    @GetMapping("/operator-association")
    public ResponseEntity<DriverOperatorAssociationResponse> active(Authentication authentication) {
        DriverOperatorAssociationResponse association =
                associationService.getActiveAssociation(authentication.getName());
        return association == null
                ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(association);
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
