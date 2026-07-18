package com.yatayat.backend.dto;

import java.time.LocalDateTime;

public record DriverTripOperationResponse(
        Long scheduledTripId,
        String routeCode,
        String routeName,
        String origin,
        String destination,
        String busName,
        String busNumber,
        String operatorName,
        LocalDateTime departureAt,
        LocalDateTime estimatedArrivalAt,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        String status,
        Long confirmedPassengers,
        Long boardedPassengers,
        Long remainingPassengers
) {
}
