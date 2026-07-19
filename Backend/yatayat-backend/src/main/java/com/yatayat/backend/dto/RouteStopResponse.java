package com.yatayat.backend.dto;

import java.math.BigDecimal;

public record RouteStopResponse(
        Long id,
        Long busStopId,
        String stopName,
        String landmark,
        Integer stopOrder,
        Integer estimatedMinutesFromStart,
        BigDecimal cumulativeFare
) {
}
