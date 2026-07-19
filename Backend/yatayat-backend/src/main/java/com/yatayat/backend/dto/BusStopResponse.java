package com.yatayat.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BusStopResponse(
        Long id,
        String name,
        BigDecimal latitude,
        BigDecimal longitude,
        String landmark,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
