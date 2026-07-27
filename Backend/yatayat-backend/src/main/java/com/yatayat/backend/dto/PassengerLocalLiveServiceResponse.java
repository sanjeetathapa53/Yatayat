package com.yatayat.backend.dto;

import com.yatayat.backend.entity.LocalServiceRunStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record PassengerLocalLiveServiceResponse(
        Long runId,
        Long routeId,
        String routeCode,
        String routeName,
        String origin,
        String destination,
        Long busId,
        String busNumber,
        String busName,
        LocalDate serviceDate,
        LocalTime plannedStartTime,
        LocalTime plannedEndTime,
        Double latitude,
        Double longitude,
        Double speed,
        Double heading,
        LocalDateTime updatedAt,
        LocalServiceRunStatus serviceStatus
) {
}
