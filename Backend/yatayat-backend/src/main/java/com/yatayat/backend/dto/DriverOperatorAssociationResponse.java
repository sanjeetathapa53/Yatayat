package com.yatayat.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public record DriverOperatorAssociationResponse(
        Long associationId,
        Long driverId,
        String driverName,
        String driverEmail,
        String driverPhone,
        String licenseNumber,
        String driverApprovalStatus,
        Long operatorId,
        String operatorName,
        String operatorEmail,
        String operatorPhone,
        String associationStatus,
        LocalDateTime invitedAt,
        LocalDateTime respondedAt,
        List<AssociatedBusResponse> assignedBuses
) {
}
