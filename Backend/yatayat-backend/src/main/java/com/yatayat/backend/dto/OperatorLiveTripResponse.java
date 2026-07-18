package com.yatayat.backend.dto;

import java.time.LocalDateTime;

public record OperatorLiveTripResponse(
        Long scheduledTripId,
        String routeCode,
        String routeName,
        String origin,
        String destination,
        String busName,
        String busNumber,
        String driverName,
        LocalDateTime departureAt,
        LocalDateTime estimatedArrivalAt,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        String status,
        Long passengerCount,
        Long boardedCount
) {
}
