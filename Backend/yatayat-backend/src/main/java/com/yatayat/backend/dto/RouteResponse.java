package com.yatayat.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RouteResponse(
        Long id,
        String code,
        String name,
        String origin,
        String destination,
        BigDecimal distanceKm,
        Integer estimatedDurationMinutes,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
