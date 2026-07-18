package com.yatayat.backend.dto;

public record TripAssignmentRequest(
        Long busId,
        Long driverId
) {
}
