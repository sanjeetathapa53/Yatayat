package com.yatayat.backend.dto;

public record PassengerRouteOptionResponse(
        Long routeId,
        String origin,
        String destination,
        String routeName
) {
}
