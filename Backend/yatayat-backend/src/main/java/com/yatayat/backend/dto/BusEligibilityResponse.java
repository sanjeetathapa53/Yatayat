package com.yatayat.backend.dto;

public record BusEligibilityResponse(
        Long id,
        String busNumber,
        String busName,
        String busType,
        Integer seatCapacity
) {
}
