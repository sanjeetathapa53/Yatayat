package com.yatayat.backend.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record LocalServiceRunRequest(
        Long routeId,
        Long busId,
        Long driverId,
        LocalDate serviceDate,
        LocalTime plannedStartTime,
        LocalTime plannedEndTime,
        String notes
) {
}
