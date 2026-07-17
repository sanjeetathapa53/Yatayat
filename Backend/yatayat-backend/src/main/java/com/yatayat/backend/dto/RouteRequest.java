package com.yatayat.backend.dto;

import com.yatayat.backend.entity.RouteStatus;
import com.yatayat.backend.entity.TripType;

import java.math.BigDecimal;

public record RouteRequest(
        String code,
        String name,
        String origin,
        String destination,
        BigDecimal distanceKm,
        Integer estimatedDurationMinutes,
        TripType tripType,
        RouteStatus status
) {
}
