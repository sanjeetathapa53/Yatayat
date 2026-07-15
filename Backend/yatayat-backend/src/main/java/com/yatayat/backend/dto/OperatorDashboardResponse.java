package com.yatayat.backend.dto;

public record OperatorDashboardResponse(
        Long operatorId,
        String organizationName,
        String registrationNumber,
        String contactEmail,
        String contactPhone,
        String address,
        String approvalStatus,
        long totalBuses,
        long pendingBuses,
        long approvedBuses,
        long totalAssociatedDrivers,
        long upcomingTrips,
        long activeTrips
) {
}
