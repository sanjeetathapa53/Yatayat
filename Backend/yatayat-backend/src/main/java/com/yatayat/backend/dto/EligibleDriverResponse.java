package com.yatayat.backend.dto;

public record EligibleDriverResponse(
        Long driverId,
        String fullName,
        String email,
        String phone,
        String licenseNumber,
        String licenseCategory,
        String driverApprovalStatus
) {
}
