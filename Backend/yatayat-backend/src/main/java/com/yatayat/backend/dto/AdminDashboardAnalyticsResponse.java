package com.yatayat.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AdminDashboardAnalyticsResponse(
        String range,
        Summary summary,
        List<DailyPoint> userRegistrations,
        List<DailyPoint> bookings,
        TripBreakdown tripBreakdown,
        List<RecentActivity> recentActivity
) {
    public record Summary(
            long totalUsers, long totalPassengers, long totalDrivers,
            long totalOperators, long totalAdmins, long usersRegisteredToday,
            long usersRegisteredInRange, long totalBuses, long activeBuses,
            long totalRoutes, long activeLocalTrips,
            long scheduledOutOfValleyTripsToday, long completedTripsToday,
            long pendingOperatorApplications, long pendingBusApprovals,
            long pendingDriverApplications, long totalBookings,
            long bookingsToday, long confirmedBookings, long cancelledBookings,
            BigDecimal verifiedTicketRevenue, BigDecimal verifiedWalletTopUpAmount,
            BigDecimal totalVerifiedPaymentAmount
    ) {}

    public record DailyPoint(LocalDate date, long count) {}

    public record TripBreakdown(long localServices, long outOfValleyTrips) {}

    public record RecentActivity(
            String type, String title, String referenceId, LocalDateTime occurredAt
    ) {}
}
