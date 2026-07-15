package com.yatayat.backend.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AdminBusResponse(
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
        String rejectionReason,
        LocalDateTime approvedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Long operatorId,
        String operatorName,
        String operatorEmail,
        String operatorPhone,
        String operatorRegistrationNumber
) {
}
