package com.yatayat.backend.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record OperatorBusResponse(
        Long id,
        String busNumber,
        String busName,
        String model,
        Integer manufactureYear,
        Integer seatCapacity,
        String busType,
        String fuelType,
        String permitNumber,
        LocalDate permitExpiryDate,
        LocalDate insuranceExpiryDate,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
