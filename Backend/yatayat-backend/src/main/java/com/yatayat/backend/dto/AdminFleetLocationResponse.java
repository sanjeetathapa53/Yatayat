package com.yatayat.backend.dto;

import com.yatayat.backend.entity.TripStatus;

import java.time.LocalDateTime;

public record AdminFleetLocationResponse(
        Long tripId,
        Long operatorId,
        String operatorName,
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
        TripStatus tripStatus
) {
}
