package com.yatayat.backend.dto;

import java.time.LocalDate;

public record OperatorBusRequest(
        String busNumber,
        String busName,
        String model,
        Integer manufactureYear,
        Integer seatCapacity,
        String busType,
        String fuelType,
        String permitNumber,
        LocalDate permitExpiryDate,
        LocalDate insuranceExpiryDate
) {
}
