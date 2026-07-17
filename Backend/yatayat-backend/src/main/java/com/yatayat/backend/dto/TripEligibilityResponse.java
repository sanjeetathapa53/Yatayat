package com.yatayat.backend.dto;

import java.util.List;

public record TripEligibilityResponse(
        List<RouteEligibilityResponse> routes,
        List<BusEligibilityResponse> buses,
        List<DriverEligibilityResponse> drivers
) {
}
