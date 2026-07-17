package com.yatayat.backend.dto;

public record DriverEligibilityResponse(
        Long id,
        String fullName,
        String licenseNumber,
        String licenseCategory
) {
}
