package com.yatayat.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AdminLiveMonitoringResponse(LocalDateTime generatedAt, List<Vehicle> vehicles) {
    public record Vehicle(
            Long busId, String busNumber, String busName, String busStatus,
            Long operatorId, String operatorName,
            Long driverId, String driverName, String driverOperationalStatus,
            Long routeId, String routeName, String origin, String destination,
            String tripType, Long operationId, String operationType, String operationStatus,
            Double latitude, Double longitude, Double speed, Double heading,
            LocalDateTime locationUpdatedAt, Long lastGpsUpdateAgeSeconds,
            String locationFreshness
    ) {}
}
