package com.yatayat.backend.dto;

import com.yatayat.backend.entity.TripStatus;

import java.time.LocalDateTime;

public record PassengerTripLocationResponse(
        Long tripId,
        BusInfo bus,
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
    public record BusInfo(Long id, String number, String name) {
    }
}
