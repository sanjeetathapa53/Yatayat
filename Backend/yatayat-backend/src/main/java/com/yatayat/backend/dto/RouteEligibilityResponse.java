package com.yatayat.backend.dto;

public record RouteEligibilityResponse(
        Long id,
        String code,
        String name,
        String origin,
        String destination
) {
}
