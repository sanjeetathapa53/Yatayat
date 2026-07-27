package com.yatayat.backend.service;

import com.yatayat.backend.dto.AdminDashboardAnalyticsResponse;
import com.yatayat.backend.dto.AdminDashboardAnalyticsResponse.*;
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
