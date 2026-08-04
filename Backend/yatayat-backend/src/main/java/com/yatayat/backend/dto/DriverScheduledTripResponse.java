package com.yatayat.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DriverScheduledTripResponse(
        Long scheduledTripId, String status,
        Long routeId, String routeName, String origin, String destination,
        Long busId, String busNumber, String busName,
        Long operatorId, String operatorName,
        LocalDateTime departureAt, LocalDateTime estimatedArrivalAt,
        BigDecimal fare, String boardingNotes,
        boolean canBoard, boolean canStart, boolean canComplete
) {}
