package com.yatayat.backend.dto;

import java.math.BigDecimal;
import java.util.List;

public record PassengerLocalRouteResponse(
        Long routeId, String routeCode, String routeName,
        String origin, String destination, BigDecimal distanceKm,
        Integer estimatedDurationMinutes, String tripType,
        List<String> stopSummary, String fareInformation
) {}
