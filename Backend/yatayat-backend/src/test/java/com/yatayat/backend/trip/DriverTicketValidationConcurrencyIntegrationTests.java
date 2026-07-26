package com.yatayat.backend.trip;

import com.yatayat.backend.entity.Ticket;
import com.yatayat.backend.entity.TicketStatus;
import com.yatayat.backend.repository.TicketRepository;
import com.yatayat.backend.service.DriverTicketValidationService;
import com.yatayat.backend.service.TicketQrTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DriverTicketValidationConcurrencyIntegrationTests {
    private static final String DRIVER_EMAIL = "qr-concurrency-driver@example.com";
    private static final String TICKET_NUMBER = "YT-TKT-CONCURRENT-001";

    @Autowired private JdbcTemplate jdbc;
    @Autowired private DriverTicketValidationService validationService;
    @Autowired private TicketQrTokenService qrTokens;
    @Autowired private TicketRepository tickets;

    @Test
    void simultaneousScansUseTicketLockAndOnlyOneSucceeds() throws Exception {
        seedTicketGraph();
        String rawToken = qrTokens.rawToken(TICKET_NUMBER);
        String payload = "{\"version\":1,\"ticketNumber\":\"" + TICKET_NUMBER
                + "\",\"token\":\"" + rawToken + "\"}";
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Future<String>> attempts = List.of(
                    executor.submit(() -> scan(ready, start, payload)),
                    executor.submit(() -> scan(ready, start, payload))
            );
            ready.await();
            start.countDown();

            List<String> results = attempts.stream().map(future -> {
                try {
                    return future.get();
                } catch (Exception exception) {
                    throw new AssertionError(exception);
                }
            }).toList();

            assertThat(results).containsExactlyInAnyOrder("VALID", "ALREADY_USED");
            Ticket persisted = tickets.findByTicketNumber(TICKET_NUMBER).orElseThrow();
            assertThat(persisted.getStatus()).isEqualTo(TicketStatus.USED);
            assertThat(persisted.getUsedAt()).isNotNull();
            assertThat(jdbc.queryForObject(
                    "select validated_by_driver_profile_id from tickets where ticket_number=?",
                    Long.class, TICKET_NUMBER)).isEqualTo(9102L);
            assertThat(jdbc.queryForObject(
                    "select validated_trip_id from tickets where ticket_number=?",
                    Long.class, TICKET_NUMBER)).isEqualTo(9107L);

            jdbc.update("update tickets set status='VALID', valid_until=? where ticket_number=?",
                    Timestamp.valueOf(LocalDateTime.now().minusMinutes(1)), TICKET_NUMBER);
            assertThat(scanWithoutBarrier(payload)).isEqualTo("EXPIRED");
            assertThat(jdbc.queryForObject(
                    "select status from tickets where ticket_number=?",
                    String.class, TICKET_NUMBER)).isEqualTo("EXPIRED");
        } finally {
            executor.shutdownNow();
        }
    }

    private String scanWithoutBarrier(String payload) {
        try {
            return validationService.validate(DRIVER_EMAIL, payload).result();
        } catch (ResponseStatusException exception) {
            return exception.getReason() == null
                    ? "UNEXPECTED" : exception.getReason().split("\\|", 2)[0];
        }
    }

    private String scan(CountDownLatch ready, CountDownLatch start, String payload)
            throws InterruptedException {
        ready.countDown();
        start.await();
        try {
            return validationService.validate(DRIVER_EMAIL, payload).result();
        } catch (ResponseStatusException exception) {
            return exception.getReason() != null && exception.getReason().startsWith("ALREADY_USED|")
                    ? "ALREADY_USED" : "UNEXPECTED:" + exception.getReason();
        }
    }

    private void seedTicketGraph() {
        LocalDateTime now = LocalDateTime.now();
        Timestamp timestamp = Timestamp.valueOf(now);
        jdbc.update("insert into users (id,email,full_name,password,phone,role) values (?,?,?,?,?,?)",
                9101L, DRIVER_EMAIL, "Concurrency Driver", "encoded", "9800009101", "DRIVER");
        jdbc.update("insert into users (id,email,full_name,password,phone,role) values (?,?,?,?,?,?)",
                9103L, "qr-concurrency-operator@example.com", "Operator", "encoded", "9800009103", "OPERATOR");
        jdbc.update("insert into users (id,email,full_name,password,phone,role) values (?,?,?,?,?,?)",
                9108L, "qr-concurrency-passenger@example.com", "Passenger", "encoded", "9800009108", "PASSENGER");
        jdbc.update("""
                insert into transport_operators
                (id,user_id,name,operator_type,registration_number,contact_person,email,phone,address,
                 verification_status,created_at,updated_at)
                values (?,?,?,?,?,?,?,?,?,?,?,?)
                """, 9104L, 9103L, "Concurrency Operator", "PRIVATE_COMPANY", "QR-CON-REG",
                "Contact", "qr-concurrency-operator@example.com", "9800009103", "Kathmandu",
                "APPROVED", timestamp, timestamp);
        jdbc.update("""
                insert into driver_profiles
                (id,user_id,license_expiry_date,license_category,citizenship_number,license_number,
                 verification_status,created_at,updated_at)
                values (?,?,?,?,?,?,?,?,?)
                """, 9102L, 9101L, java.sql.Date.valueOf(now.toLocalDate().plusYears(1)),
                "A", "QR-CON-CIT", "QR-CON-LIC", "APPROVED", timestamp, timestamp);
        jdbc.update("""
                insert into routes
                (id,code,name,origin,destination,distance_km,estimated_duration_minutes,status,
                 trip_type,created_at,updated_at)
                values (?,?,?,?,?,?,?,?,?,?,?)
                """, 9105L, "QR-CON-ROUTE", "Concurrency Route", "Kathmandu", "Pokhara",
                200, 360, "ACTIVE", "OUT_OF_VALLEY", timestamp, timestamp);
        jdbc.update("""
                insert into buses
                (id,bus_number,bus_name,seat_capacity,bus_type,status,operator_id,operator_name,
                 created_at,updated_at)
                values (?,?,?,?,?,?,?,?,?,?)
                """, 9106L, "QR-CON-BUS", "Concurrency Bus", 40, "DELUXE", "APPROVED",
                9104L, "Concurrency Operator", timestamp, timestamp);
        jdbc.update("""
                insert into driver_operator_associations
                (id,driver_profile_id,operator_id,status,invited_at,created_at,updated_at)
                values (?,?,?,?,?,?,?)
                """, 9112L, 9102L, 9104L, "ACTIVE", timestamp, timestamp, timestamp);
        jdbc.update("""
                insert into scheduled_trips
                (id,operator_id,route_id,bus_id,driver_profile_id,departure_at,estimated_arrival_at,
                 fare,seat_capacity_snapshot,status,created_at,updated_at,version)
                values (?,?,?,?,?,?,?,?,?,?,?,?,?)
                """, 9107L, 9104L, 9105L, 9106L, 9102L,
                Timestamp.valueOf(now.minusMinutes(10)), Timestamp.valueOf(now.plusHours(6)),
                500, 40, "BOARDING", timestamp, timestamp, 0);
        jdbc.update("""
                insert into passenger_trip_bookings
                (id,booking_reference,passenger_id,scheduled_trip_id,passenger_name,passenger_phone,
                 number_of_seats,fare_per_seat,total_fare,status,booked_at,created_at,updated_at,version)
                values (?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """, 9109L, "YAT-QR-CONCURRENT", 9108L, 9107L, "Passenger", "9800009108",
                1, 500, 500, "CONFIRMED", timestamp, timestamp, timestamp, 0);
        jdbc.update("""
                insert into payments
                (id,booking_id,passenger_id,amount,payment_method,status,transaction_reference,
                 paid_at,created_at,updated_at)
                values (?,?,?,?,?,?,?,?,?,?)
                """, 9110L, 9109L, 9108L, 500, "WALLET", "SUCCESS", "PAY-QR-CONCURRENT",
                timestamp, timestamp, timestamp);
        jdbc.update("""
                insert into tickets
                (id,ticket_number,booking_id,status,qr_token_hash,issued_at,valid_from,valid_until,
                 auto_email_status,created_at,updated_at)
                values (?,?,?,?,?,?,?,?,?,?,?)
                """, 9111L, TICKET_NUMBER, 9109L, "VALID", qrTokens.storedHash(TICKET_NUMBER),
                timestamp, Timestamp.valueOf(now.minusMinutes(5)), Timestamp.valueOf(now.plusHours(7)),
                "PENDING", timestamp, timestamp);
    }
}
