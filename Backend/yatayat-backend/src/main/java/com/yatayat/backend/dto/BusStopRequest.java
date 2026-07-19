package com.yatayat.backend.dto;

import java.math.BigDecimal;

public record BusStopRequest(
        String name,
        BigDecimal latitude,
        BigDecimal longitude,
        String landmark,
        Boolean active
) {
}
