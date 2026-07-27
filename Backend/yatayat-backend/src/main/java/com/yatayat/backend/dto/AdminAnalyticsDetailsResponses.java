package com.yatayat.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class AdminAnalyticsDetailsResponses {
    private AdminAnalyticsDetailsResponses() {}

    public record DailyCount(LocalDate date, long count) {}
    public record DailyAmount(LocalDate date, BigDecimal amount) {}
    public record RecentUser(Long id, String role, String provider, LocalDateTime createdAt) {}
    public record RecentBooking(String bookingReference, String status,
                                BigDecimal totalFare, LocalDateTime bookedAt) {}
    public record RecentTransaction(String source, String method, BigDecimal amount,
                                    LocalDateTime verifiedAt) {}

    public record UsersResponse(
            String range, long totalUsers, long passengers, long drivers,
            long operators, long admins, long registrationsToday,
            long registrationsInRange, List<DailyCount> dailyRegistrations,
            Map<String, Long> roleDistribution,
            Map<String, Long> providerDistribution,
            List<RecentUser> recentRegistrations
    ) {}

    public record OperationsResponse(
            String range, long totalBuses, long activeBuses, long pendingBuses,
            long totalRoutes, long activeLocalServices, long scheduledTrips,
            long completedTrips, Map<String, Long> tripTypeBreakdown,
            Map<String, Long> tripStatusBreakdown, List<DailyCount> dailyTrips
    ) {}

    public record BookingsResponse(
            String range, long totalBookings, long bookingsToday,
            long confirmedBookings, long cancelledBookings,
            List<DailyCount> dailyBookings, Map<String, Long> statusDistribution,
            Map<String, Long> tripTypeBreakdown, List<RecentBooking> recentBookings
    ) {}

    public record RevenueResponse(
            String range, BigDecimal ticketRevenue, BigDecimal walletTopUpRevenue,
            BigDecimal totalVerifiedRevenue, List<DailyAmount> dailyRevenue,
            Map<String, BigDecimal> sourceBreakdown, long successfulPayments,
            long pendingPayments, long failedPayments,
            List<RecentTransaction> recentTransactions
    ) {}
}
