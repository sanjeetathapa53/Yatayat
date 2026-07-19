package com.yatayat.backend.dto;

import java.math.BigDecimal;

public record RouteStopRequest(
        Long busStopId,
        Integer stopOrder,
        Integer estimatedMinutesFromStart,
        BigDecimal cumulativeFare
) {
}
