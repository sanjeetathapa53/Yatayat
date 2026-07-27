package com.yatayat.backend.localfare;

import com.yatayat.backend.entity.LocalFarePassStatus;
import com.yatayat.backend.repository.LocalFarePassRepository;
import com.yatayat.backend.service.DriverLocalFarePassValidationService;
import com.yatayat.backend.service.LocalFarePassQrTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class LocalFarePassConcurrencyIntegrationTests {
    private static final String DRIVER_EMAIL = "local-pass-concurrency-driver@example.com";
    private static final String PASS_NUMBER = "YT-LFP-CONCURRENT-001";

    @Autowired JdbcTemplate jdbc;
    @Autowired DriverLocalFarePassValidationService validationService;
    @Autowired LocalFarePassQrTokenService tokens;
    @Autowired LocalFarePassRepository passes;

    @Test
    void simultaneousScansLockPassAndOnlyOneSucceeds() throws Exception {
        seed();
        String payload = "{\"version\":1,\"type\":\"LOCAL_FARE_PASS\",\"passNumber\":\""
                + PASS_NUMBER + "\",\"token\":\"" + tokens.rawToken(PASS_NUMBER) + "\"}";
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Future<String>> attempts = List.of(
                    executor.submit(() -> scan(ready, start, payload)),
                    executor.submit(() -> scan(ready, start, payload)));
            ready.await();
            start.countDown();
            List<String> results = attempts.stream().map(this::result).toList();
            assertThat(results).containsExactlyInAnyOrder("VALID", "ALREADY_USED");
            var persisted = passes.findById(9220L).orElseThrow();
            assertThat(persisted.getStatus()).isEqualTo(LocalFarePassStatus.USED);
            assertThat(persisted.getUsedAt()).isNotNull();
            assertThat(persisted.getValidatedByDriverProfile().getId()).isEqualTo(9202L);
            assertThat(persisted.getValidatedLocalServiceRun().getId()).isEqualTo(9210L);
        } finally {
            executor.shutdownNow();
        }
    }

    private String scan(CountDownLatch ready, CountDownLatch start, String payload)
            throws InterruptedException {
        ready.countDown();
        start.await();
        try {
            return validationService.validate(DRIVER_EMAIL, payload).result();
        } catch (ResponseStatusException exception) {
            return exception.getReason() == null
                    ? "UNEXPECTED" : exception.getReason().split("\\|", 2)[0];
        }
    }

    private String result(Future<String> future) {
        try {
            return future.get();
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }

    private void seed() {
        LocalDateTime now = LocalDateTime.now();
        Timestamp timestamp = Timestamp.valueOf(now);
        jdbc.update("insert into users (id,email,full_name,password,phone,role) values (?,?,?,?,?,?)",
                9201L, DRIVER_EMAIL, "Local Driver", "encoded", "9800009201", "DRIVER");
        jdbc.update("insert into users (id,email,full_name,password,phone,role) values (?,?,?,?,?,?)",
                9203L, "local-pass-operator@example.com", "Operator", "encoded", "9800009203", "OPERATOR");
        jdbc.update("insert into users (id,email,full_name,password,phone,role) values (?,?,?,?,?,?)",
                9208L, "local-pass-passenger@example.com", "Passenger", "encoded", "9800009208", "PASSENGER");
        jdbc.update("""
                insert into transport_operators
                (id,user_id,name,operator_type,registration_number,contact_person,email,phone,address,
                 verification_status,created_at,updated_at)
                values (?,?,?,?,?,?,?,?,?,?,?,?)
                """, 9204L, 9203L, "Local Operator", "PRIVATE_COMPANY", "LFP-CON-REG",
                "Contact", "local-pass-operator@example.com", "9800009203", "Kathmandu",
                "APPROVED", timestamp, timestamp);
        jdbc.update("""
                insert into driver_profiles
                (id,user_id,license_expiry_date,license_category,citizenship_number,license_number,
                 verification_status,created_at,updated_at)
                values (?,?,?,?,?,?,?,?,?)
                """, 9202L, 9201L, Date.valueOf(now.toLocalDate().plusYears(1)),
                "A", "LFP-CON-CIT", "LFP-CON-LIC", "APPROVED", timestamp, timestamp);
        jdbc.update("""
                insert into routes
                (id,code,name,origin,destination,distance_km,estimated_duration_minutes,status,
                 trip_type,created_at,updated_at)
                values (?,?,?,?,?,?,?,?,?,?,?)
                """, 9205L, "LFP-CON-ROUTE", "Local Route", "Stop A", "Stop B",
                10, 40, "ACTIVE", "LOCAL", timestamp, timestamp);
        jdbc.update("""
                insert into buses
                (id,bus_number,bus_name,seat_capacity,bus_type,status,operator_id,operator_name,
                 created_at,updated_at)
                values (?,?,?,?,?,?,?,?,?,?)
                """, 9206L, "LFP-CON-BUS", "Local Bus", 30, "LOCAL", "APPROVED",
                9204L, "Local Operator", timestamp, timestamp);
        jdbc.update("""
                insert into driver_operator_associations
                (id,driver_profile_id,operator_id,status,invited_at,created_at,updated_at)
                values (?,?,?,?,?,?,?)
                """, 9207L, 9202L, 9204L, "ACTIVE", timestamp, timestamp, timestamp);
        jdbc.update("""
                insert into local_service_runs
                (id,operator_id,route_id,bus_id,driver_profile_id,service_date,planned_start_time,
                 planned_end_time,actual_started_at,status,created_at,updated_at,version)
                values (?,?,?,?,?,?,?,?,?,?,?,?,?)
                """, 9210L, 9204L, 9205L, 9206L, 9202L, Date.valueOf(now.toLocalDate()),
                Time.valueOf("08:00:00"), Time.valueOf("18:00:00"), timestamp,
                "IN_SERVICE", timestamp, timestamp, 0);
        jdbc.update("""
                insert into bus_stops
                (id,name,normalized_name,active,created_at,updated_at)
                values (?,?,?,?,?,?)
                """, 9211L, "Stop A", "STOP A", true, timestamp, timestamp);
        jdbc.update("""
                insert into bus_stops
                (id,name,normalized_name,active,created_at,updated_at)
                values (?,?,?,?,?,?)
                """, 9212L, "Stop B", "STOP B", true, timestamp, timestamp);
        jdbc.update("insert into wallets (id,balance,wallet_pin,user_id) values (?,?,?,?)",
                9213L, 100.0, "encoded", 9208L);
        jdbc.update("""
                insert into wallet_transactions
                (id,type,amount,status,payment_method,transaction_date,wallet_id)
                values (?,?,?,?,?,?,?)
                """, 9214L, "LOCAL_FARE_PAYMENT", 25.0, "SUCCESS", "WALLET", timestamp, 9213L);
        jdbc.update("""
                insert into local_fare_passes
                (id,pass_number,passenger_id,route_id,boarding_stop_id,destination_stop_id,
                 boarding_stop_order,destination_stop_order,boarding_stop_name,destination_stop_name,
                 fare,status,wallet_transaction_id,issued_at,valid_from,valid_until,qr_token_hash,
                 created_at,updated_at,version)
                values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """, 9220L, PASS_NUMBER, 9208L, 9205L, 9211L, 9212L,
                1, 2, "Stop A", "Stop B", 25.0, "VALID", 9214L,
                timestamp, Timestamp.valueOf(now.minusMinutes(1)),
                Timestamp.valueOf(now.plusHours(24)), tokens.storedHash(PASS_NUMBER),
                timestamp, timestamp, 0);
    }
}
