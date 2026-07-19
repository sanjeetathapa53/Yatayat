package com.yatayat.backend.dto;

import com.yatayat.backend.entity.RouteStatus;
import com.yatayat.backend.entity.TripType;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

public record RouteRequest(
        String code,
        String name,
        String origin,
        String destination,
        BigDecimal distanceKm,
        Integer estimatedDurationMinutes,
        LocalTime operatingStartTime,
        LocalTime operatingEndTime,
        TripType tripType,
        RouteStatus status,
        List<RouteStopRequest> stops
) {
}
