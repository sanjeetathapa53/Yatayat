package com.yatayat.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TripResponse(
        Long id,
        Long routeId,
        String routeCode,
        String routeName,
        String origin,
        String destination,
        Long busId,
        String busNumber,
        String busName,
        Long driverId,
        String driverName,
        LocalDateTime departureAt,
        LocalDateTime estimatedArrivalAt,
        LocalDateTime actualDepartureAt,
        LocalDateTime actualArrivalAt,
        BigDecimal fare,
        Integer seatCapacity,
        String status,
        String boardingNotes,
        String cancellationReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
