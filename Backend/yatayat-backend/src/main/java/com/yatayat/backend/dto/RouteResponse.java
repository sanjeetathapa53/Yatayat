package com.yatayat.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public record RouteResponse(
        Long id,
        String code,
        String name,
        String origin,
        String destination,
        BigDecimal distanceKm,
        Integer estimatedDurationMinutes,
        LocalTime operatingStartTime,
        LocalTime operatingEndTime,
        String tripType,
        String status,
        List<RouteStopResponse> stops,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
