package com.yatayat.backend.dto;

import com.yatayat.backend.entity.LocalServiceRunStatus;

import java.time.LocalDateTime;

public record OperatorLocalFleetLocationResponse(
        Long runId,
        Long busId,
        String busNumber,
        String busName,
        Long driverId,
        String driverName,
        Long routeId,
        String routeName,
        String origin,
        String destination,
        Double latitude,
        Double longitude,
        Double speed,
        Double heading,
        LocalDateTime updatedAt,
        LocalServiceRunStatus serviceStatus
) {
}
