package com.yatayat.backend.dto;

import java.time.LocalDateTime;

public record TripLocationResponse(
        Long tripId,
        Double latitude,
        Double longitude,
        Double accuracy,
        Double speed,
        Double heading,
        LocalDateTime updatedAt
) {
}
