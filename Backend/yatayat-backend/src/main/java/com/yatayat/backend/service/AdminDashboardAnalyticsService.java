package com.yatayat.backend.service;

import com.yatayat.backend.dto.AdminDashboardAnalyticsResponse;
import com.yatayat.backend.dto.AdminDashboardAnalyticsResponse.*;
import com.yatayat.backend.dto.AdminAnalyticsDetailsResponses.*;
import com.yatayat.backend.entity.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class AdminDashboardAnalyticsService {
    @PersistenceContext
    private EntityManager entityManager;

    public AdminDashboardAnalyticsResponse dashboard(String requestedRange) {
        int days = switch (requestedRange == null ? "LAST_7_DAYS" : requestedRange) {
            case "LAST_7_DAYS" -> 7;
            case "LAST_30_DAYS" -> 30;
            default -> throw new IllegalArgumentException(
                    "Range must be LAST_7_DAYS or LAST_30_DAYS.");
        };
        String range = days == 7 ? "LAST_7_DAYS" : "LAST_30_DAYS";
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(days - 1L);
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime tomorrow = today.plusDays(1).atStartOfDay();

        BigDecimal ticketRevenue = sum(
                "select coalesce(sum(p.amount), 0) from Payment p "
                        + "where p.status = :status and p.verifiedAt is not null",
                Map.of("status", PaymentStatus.SUCCESS));
        BigDecimal topUpRevenue = sum(
                "select coalesce(sum(t.amount), 0) from WalletTopUp t "
                        + "where t.status = :status and t.verifiedAt is not null "
                        + "and t.creditedAt is not null",
                Map.of("status", PaymentStatus.SUCCESS));

        Summary summary = new Summary(
                count("select count(u) from User u", Map.of()),
                roleCount("PASSENGER"), roleCount("DRIVER"), roleCount("OPERATOR"),
                roleCount("ADMIN"),
                countBetween("User", "createdAt", today.atStartOfDay(), tomorrow),
                countBetween("User", "createdAt", start, tomorrow),
                count("select count(b) from Bus b", Map.of()),
                count("select count(b) from Bus b where b.status = :status",
                        Map.of("status", BusStatus.ACTIVE)),
                count("select count(r) from Route r", Map.of()),
                count("select count(r) from LocalServiceRun r where r.status = :status",
                        Map.of("status", LocalServiceRunStatus.IN_SERVICE)),
                count("select count(t) from ScheduledTrip t where t.departureAt >= :start "
                                + "and t.departureAt < :end and t.status = :status",
                        Map.of("start", today.atStartOfDay(), "end", tomorrow,
                                "status", TripStatus.SCHEDULED)),
                count("select count(t) from ScheduledTrip t where t.actualArrivalAt >= :start "
                                + "and t.actualArrivalAt < :end and t.status = :status",
                        Map.of("start", today.atStartOfDay(), "end", tomorrow,
                                "status", TripStatus.COMPLETED))
                        + count("select count(r) from LocalServiceRun r where r.actualCompletedAt >= :start "
                                        + "and r.actualCompletedAt < :end and r.status = :status",
                                Map.of("start", today.atStartOfDay(), "end", tomorrow,
                                        "status", LocalServiceRunStatus.COMPLETED)),
                count("select count(o) from TransportOperator o where o.verificationStatus = :status",
                        Map.of("status", OperatorVerificationStatus.PENDING)),
                count("select count(b) from Bus b where b.status = :status",
                        Map.of("status", BusStatus.PENDING)),
                count("select count(d) from DriverProfile d where d.verificationStatus = :status",
                        Map.of("status", DriverVerificationStatus.PENDING)),
                count("select count(b) from PassengerTripBooking b", Map.of()),
                countBetween("PassengerTripBooking", "bookedAt", today.atStartOfDay(), tomorrow),
                count("select count(b) from PassengerTripBooking b where b.status = :status",
                        Map.of("status", BookingStatus.CONFIRMED)),
                count("select count(b) from PassengerTripBooking b where b.status = :status",
                        Map.of("status", BookingStatus.CANCELLED)),
                ticketRevenue, topUpRevenue, ticketRevenue.add(topUpRevenue)
        );

        return new AdminDashboardAnalyticsResponse(
                range, summary,
                dailySeries("User", "createdAt", startDate, today),
                dailySeries("PassengerTripBooking", "bookedAt", startDate, today),
                new TripBreakdown(
                        countBetween("LocalServiceRun", "createdAt", start, tomorrow),
                        countBetween("ScheduledTrip", "createdAt", start, tomorrow)),
                recentActivity()
        );
    }

    public UsersResponse users(String requestedRange) {
        RangeWindow window = range(requestedRange);
        var dashboard = dashboard(window.name());
        Summary summary = dashboard.summary();
        Map<String, Long> roles = linkedCounts(
                "PASSENGER", summary.totalPassengers(), "DRIVER", summary.totalDrivers(),
                "OPERATOR", summary.totalOperators(), "ADMIN", summary.totalAdmins());
        Map<String, Long> providers = new LinkedHashMap<>();
        for (AuthenticationProvider provider : AuthenticationProvider.values()) {
            providers.put(provider.name(), count(
                    "select count(u) from User u where u.authenticationProvider = :provider",
                    Map.of("provider", provider)));
        }
        List<RecentUser> recent = entityManager.createQuery(
                        "select u from User u where u.createdAt is not null order by u.createdAt desc",
                        User.class).setMaxResults(10).getResultList().stream()
                .map(user -> new RecentUser(user.getId(), safeRole(user.getRole()).toUpperCase(Locale.ROOT),
                        user.getAuthenticationProvider().name(), user.getCreatedAt())).toList();
        return new UsersResponse(window.name(), summary.totalUsers(), summary.totalPassengers(),
                summary.totalDrivers(), summary.totalOperators(), summary.totalAdmins(),
                summary.usersRegisteredToday(), summary.usersRegisteredInRange(),
                toDailyCounts(dashboard.userRegistrations()), roles, providers, recent);
    }

    public OperationsResponse operations(String requestedRange) {
        RangeWindow window = range(requestedRange);
        var dashboard = dashboard(window.name());
        Summary summary = dashboard.summary();
        Map<String, Long> statuses = new LinkedHashMap<>();
        for (TripStatus status : TripStatus.values()) {
            statuses.put("OUT_OF_VALLEY_" + status.name(), count(
                    "select count(t) from ScheduledTrip t where t.status = :status",
                    Map.of("status", status)));
        }
        for (LocalServiceRunStatus status : LocalServiceRunStatus.values()) {
            statuses.put("LOCAL_" + status.name(), count(
                    "select count(t) from LocalServiceRun t where t.status = :status",
                    Map.of("status", status)));
        }
        List<DailyCount> daily = combineDaily(
                dailySeries("LocalServiceRun", "createdAt", window.startDate(), window.today()),
                dailySeries("ScheduledTrip", "createdAt", window.startDate(), window.today()));
        long scheduled = count("select count(t) from ScheduledTrip t where t.status = :status",
                Map.of("status", TripStatus.SCHEDULED));
        long completed = count("select count(t) from ScheduledTrip t where t.status = :status",
                Map.of("status", TripStatus.COMPLETED))
                + count("select count(t) from LocalServiceRun t where t.status = :status",
                Map.of("status", LocalServiceRunStatus.COMPLETED));
        return new OperationsResponse(window.name(), summary.totalBuses(), summary.activeBuses(),
                summary.pendingBusApprovals(), summary.totalRoutes(), summary.activeLocalTrips(),
                scheduled, completed,
                linkedCounts("LOCAL", dashboard.tripBreakdown().localServices(),
                        "OUT_OF_VALLEY", dashboard.tripBreakdown().outOfValleyTrips()),
                statuses, daily);
    }

    public BookingsResponse bookings(String requestedRange) {
        RangeWindow window = range(requestedRange);
        var dashboard = dashboard(window.name());
        Summary summary = dashboard.summary();
        Map<String, Long> statuses = new LinkedHashMap<>();
        for (BookingStatus status : BookingStatus.values()) {
            statuses.put(status.name(), count(
                    "select count(b) from PassengerTripBooking b where b.status = :status",
                    Map.of("status", status)));
        }
        List<RecentBooking> recent = entityManager.createQuery(
                        "select b from PassengerTripBooking b order by b.bookedAt desc",
                        PassengerTripBooking.class).setMaxResults(10).getResultList().stream()
                .map(booking -> new RecentBooking(booking.getBookingReference(),
                        booking.getStatus().name(), booking.getTotalFare(), booking.getBookedAt()))
                .toList();
        return new BookingsResponse(window.name(), summary.totalBookings(), summary.bookingsToday(),
                summary.confirmedBookings(), summary.cancelledBookings(),
                toDailyCounts(dashboard.bookings()), statuses,
                Map.of("OUT_OF_VALLEY", summary.totalBookings()), recent);
    }

    public RevenueResponse revenue(String requestedRange) {
        RangeWindow window = range(requestedRange);
        LocalDateTime start = window.startDate().atStartOfDay();
        LocalDateTime end = window.today().plusDays(1).atStartOfDay();
        BigDecimal ticket = verifiedTicketRevenue(start, end);
        BigDecimal topUp = verifiedTopUpRevenue(start, end);
        Map<LocalDate, BigDecimal> dailyTotals = dailyVerifiedAmounts(
                "Payment", "", start, end);
        dailyVerifiedAmounts("WalletTopUp", "and e.creditedAt is not null", start, end)
                .forEach((date, amount) -> dailyTotals.merge(date, amount, BigDecimal::add));
        List<DailyAmount> daily = new ArrayList<>();
        for (LocalDate date = window.startDate(); !date.isAfter(window.today()); date = date.plusDays(1)) {
            daily.add(new DailyAmount(date, dailyTotals.getOrDefault(date, BigDecimal.ZERO)));
        }
        long successful = paymentAttemptCount(PaymentStatus.SUCCESS, start, end);
        long pending = paymentAttemptCount(PaymentStatus.PENDING, start, end)
                + paymentAttemptCount(PaymentStatus.INITIATED, start, end);
        long failed = paymentAttemptCount(PaymentStatus.FAILED, start, end);
        List<RecentTransaction> recent = new ArrayList<>();
        entityManager.createQuery("select p from Payment p where p.status = :status "
                                + "and p.verifiedAt is not null order by p.verifiedAt desc", Payment.class)
                .setParameter("status", PaymentStatus.SUCCESS).setMaxResults(10).getResultList()
                .forEach(payment -> recent.add(new RecentTransaction("TICKET_PAYMENT",
                        payment.getPaymentMethod().name(), payment.getAmount(), payment.getVerifiedAt())));
        entityManager.createQuery("select t from WalletTopUp t where t.status = :status "
                                + "and t.verifiedAt is not null and t.creditedAt is not null "
                                + "order by t.verifiedAt desc", WalletTopUp.class)
                .setParameter("status", PaymentStatus.SUCCESS).setMaxResults(10).getResultList()
                .forEach(topUpItem -> recent.add(new RecentTransaction("WALLET_TOP_UP",
                        topUpItem.getPaymentMethod().name(), topUpItem.getAmount(), topUpItem.getVerifiedAt())));
        List<RecentTransaction> sorted = recent.stream()
                .sorted(Comparator.comparing(RecentTransaction::verifiedAt).reversed())
                .limit(10).toList();
        return new RevenueResponse(window.name(), ticket, topUp, ticket.add(topUp), daily,
                linkedAmounts("TICKET_PAYMENT", ticket, "WALLET_TOP_UP", topUp),
                successful, pending, failed, sorted);
    }

    private RangeWindow range(String requestedRange) {
        String value = requestedRange == null ? "LAST_7_DAYS" : requestedRange;
        int days = switch (value) {
            case "LAST_7_DAYS" -> 7;
            case "LAST_30_DAYS" -> 30;
            default -> throw new IllegalArgumentException(
                    "Range must be LAST_7_DAYS or LAST_30_DAYS.");
        };
        LocalDate today = LocalDate.now();
        return new RangeWindow(value, today.minusDays(days - 1L), today);
    }

    private BigDecimal verifiedTicketRevenue(LocalDateTime start, LocalDateTime end) {
        return sum("select coalesce(sum(p.amount), 0) from Payment p where p.status = :status "
                        + "and p.verifiedAt >= :start and p.verifiedAt < :end",
                Map.of("status", PaymentStatus.SUCCESS, "start", start, "end", end));
    }

    private BigDecimal verifiedTopUpRevenue(LocalDateTime start, LocalDateTime end) {
        return sum("select coalesce(sum(t.amount), 0) from WalletTopUp t where t.status = :status "
                        + "and t.verifiedAt >= :start and t.verifiedAt < :end and t.creditedAt is not null",
                Map.of("status", PaymentStatus.SUCCESS, "start", start, "end", end));
    }

    private Map<LocalDate, BigDecimal> dailyVerifiedAmounts(
            String entity, String extraPredicate, LocalDateTime start, LocalDateTime end) {
        List<Object[]> rows = entityManager.createQuery(
                        "select cast(e.verifiedAt as LocalDate), coalesce(sum(e.amount), 0) from "
                                + entity + " e where e.status = :status and e.verifiedAt >= :start "
                                + "and e.verifiedAt < :end " + extraPredicate
                                + " group by cast(e.verifiedAt as LocalDate)", Object[].class)
                .setParameter("status", PaymentStatus.SUCCESS)
                .setParameter("start", start)
                .setParameter("end", end)
                .getResultList();
        Map<LocalDate, BigDecimal> result = new HashMap<>();
        for (Object[] row : rows) result.put((LocalDate) row[0], (BigDecimal) row[1]);
        return result;
    }

    private long paymentAttemptCount(PaymentStatus status, LocalDateTime start, LocalDateTime end) {
        Map<String, Object> parameters = Map.of("status", status, "start", start, "end", end);
        return count("select count(p) from Payment p where p.status = :status "
                        + "and p.createdAt >= :start and p.createdAt < :end", parameters)
                + count("select count(t) from WalletTopUp t where t.status = :status "
                        + "and t.createdAt >= :start and t.createdAt < :end", parameters);
    }

    private List<DailyCount> toDailyCounts(List<DailyPoint> points) {
        return points.stream().map(point -> new DailyCount(point.date(), point.count())).toList();
    }

    private List<DailyCount> combineDaily(List<DailyPoint> first, List<DailyPoint> second) {
        List<DailyCount> result = new ArrayList<>();
        for (int index = 0; index < first.size(); index++) {
            result.add(new DailyCount(first.get(index).date(),
                    first.get(index).count() + second.get(index).count()));
        }
        return result;
    }

    private Map<String, Long> linkedCounts(Object... values) {
        Map<String, Long> result = new LinkedHashMap<>();
        for (int index = 0; index < values.length; index += 2) {
            result.put((String) values[index], (Long) values[index + 1]);
        }
        return result;
    }

    private Map<String, BigDecimal> linkedAmounts(Object... values) {
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (int index = 0; index < values.length; index += 2) {
            result.put((String) values[index], (BigDecimal) values[index + 1]);
        }
        return result;
    }

    private record RangeWindow(String name, LocalDate startDate, LocalDate today) {}

    private long roleCount(String role) {
        return count("select count(u) from User u where upper(u.role) = :role",
                Map.of("role", role));
    }

    private long countBetween(String entity, String field,
                              LocalDateTime start, LocalDateTime end) {
        return count("select count(e) from " + entity + " e where e." + field
                        + " >= :start and e." + field + " < :end",
                Map.of("start", start, "end", end));
    }

    private List<DailyPoint> dailySeries(String entity, String field,
                                         LocalDate first, LocalDate last) {
        LocalDateTime start = first.atStartOfDay();
        LocalDateTime end = last.plusDays(1).atStartOfDay();
        List<Object[]> rows = entityManager.createQuery(
                        "select cast(e." + field + " as LocalDate), count(e) from "
                                + entity + " e where e." + field
                                + " >= :start and e." + field + " < :end "
                                + "group by cast(e." + field + " as LocalDate)",
                        Object[].class)
                .setParameter("start", start)
                .setParameter("end", end)
                .getResultList();
        Map<LocalDate, Long> counts = new HashMap<>();
        for (Object[] row : rows) {
            counts.put((LocalDate) row[0], (Long) row[1]);
        }
        List<DailyPoint> points = new ArrayList<>();
        for (LocalDate date = first; !date.isAfter(last); date = date.plusDays(1)) {
            points.add(new DailyPoint(date, counts.getOrDefault(date, 0L)));
        }
        return points;
    }

    private List<RecentActivity> recentActivity() {
        List<RecentActivity> activity = new ArrayList<>();
        entityManager.createQuery(
                        "select u from User u where u.createdAt is not null order by u.createdAt desc",
                        User.class)
                .setMaxResults(5).getResultList()
                .forEach(user -> activity.add(new RecentActivity(
                        "USER_REGISTERED", "New " + safeRole(user.getRole()) + " account registered",
                        String.valueOf(user.getId()), user.getCreatedAt())));
        entityManager.createQuery(
                        "select b from PassengerTripBooking b order by b.createdAt desc",
                        PassengerTripBooking.class)
                .setMaxResults(5).getResultList()
                .forEach(booking -> activity.add(new RecentActivity(
                        "BOOKING", "Booking " + booking.getStatus().name().toLowerCase(Locale.ROOT),
                        booking.getBookingReference(), booking.getCreatedAt())));
        entityManager.createQuery(
                        "select p from Payment p where p.status = :status and p.verifiedAt is not null "
                                + "order by p.verifiedAt desc", Payment.class)
                .setParameter("status", PaymentStatus.SUCCESS)
                .setMaxResults(5).getResultList()
                .forEach(payment -> activity.add(new RecentActivity(
                        "PAYMENT_VERIFIED", "Ticket payment verified",
                        payment.getTransactionReference(), payment.getVerifiedAt())));
        return activity.stream()
                .filter(item -> item.occurredAt() != null)
                .sorted(Comparator.comparing(RecentActivity::occurredAt).reversed())
                .limit(10).toList();
    }

    private String safeRole(String role) {
        if (role == null) return "user";
        return switch (role.toUpperCase(Locale.ROOT)) {
            case "PASSENGER" -> "passenger";
            case "DRIVER" -> "driver";
            case "OPERATOR" -> "operator";
            case "ADMIN" -> "admin";
            default -> "user";
        };
    }

    private long count(String jpql, Map<String, Object> parameters) {
        var query = entityManager.createQuery(jpql, Long.class);
        parameters.forEach(query::setParameter);
        return query.getSingleResult();
    }

    private BigDecimal sum(String jpql, Map<String, Object> parameters) {
        var query = entityManager.createQuery(jpql, BigDecimal.class);
        parameters.forEach(query::setParameter);
        return query.getSingleResult();
    }
}
