package com.yatayat.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TripSummaryResponse(
        Long id,
        String routeCode,
        String routeName,
        String origin,
        String destination,
        String busNumber,
        String driverName,
        LocalDateTime departureAt,
        LocalDateTime estimatedArrivalAt,
        BigDecimal fare,
        Integer seatCapacity,
        String status
) {
}
