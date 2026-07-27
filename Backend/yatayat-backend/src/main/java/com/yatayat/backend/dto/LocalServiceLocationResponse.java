package com.yatayat.backend.dto;

import java.time.LocalDateTime;

public record LocalServiceLocationResponse(
        Long runId,
        Double latitude,
        Double longitude,
        Double accuracy,
        Double speed,
        Double heading,
        LocalDateTime updatedAt
) {
}
