package com.yatayat.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TripCreateRequest(
        Long routeId,
        Long busId,
        Long driverId,
        LocalDateTime departureAt,
        LocalDateTime estimatedArrivalAt,
        BigDecimal fare,
        String boardingNotes
) {
}
