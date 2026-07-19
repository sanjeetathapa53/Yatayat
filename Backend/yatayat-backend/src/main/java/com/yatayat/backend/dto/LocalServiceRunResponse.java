package com.yatayat.backend.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public record LocalServiceRunResponse(
        Long id,
        Long routeId,
        String routeCode,
        String routeName,
        String origin,
        String destination,
        Long busId,
        String busNumber,
        String busName,
        Integer seatCapacity,
        Long driverId,
        String driverName,
        String driverLicenseCategory,
        LocalDate serviceDate,
        LocalTime plannedStartTime,
        LocalTime plannedEndTime,
        String status,
        String notes,
        LocalDateTime actualStartedAt,
        LocalDateTime actualCompletedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<RouteStopResponse> orderedStops
) {
}
