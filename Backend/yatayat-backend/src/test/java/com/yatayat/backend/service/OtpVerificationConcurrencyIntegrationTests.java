package com.yatayat.backend.service;

import com.yatayat.backend.entity.*;
import com.yatayat.backend.repository.OtpVerificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class OtpVerificationConcurrencyIntegrationTests {
    @Autowired OtpVerificationService service;
    @Autowired OtpVerificationRepository repository;
    @Autowired PasswordEncoder passwordEncoder;
    @MockitoBean EmailService emailService;

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    @Test
    void concurrentPasswordResetVerificationConsumesOtpOnlyOnce() throws Exception {
        saveIssued("passenger@example.com", OtpPurpose.PASSWORD_RESET, "123456");
        assertThat(runConcurrently(() -> service.verifyAndConsume(
                "passenger@example.com", "123456", OtpPurpose.PASSWORD_RESET)))
                .isEqualTo(1);
    }

    @Test
    void concurrentRegistrationCannotConsumeVerifiedStateTwice() throws Exception {
        OtpVerification verification = saveIssued(
                "passenger@example.com", OtpPurpose.REGISTRATION, "123456");
        verification.setStatus(OtpVerificationStatus.VERIFIED);
        verification.setVerifiedAt(LocalDateTime.now());
        verification.setOtpHash(null);
        repository.saveAndFlush(verification);

        assertThat(runConcurrently(() ->
                service.consumeVerifiedRegistration("passenger@example.com")))
                .isEqualTo(1);
    }

    private int runConcurrently(Runnable action) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Boolean>> futures = List.of(
                    executor.submit(() -> run(action, ready, start)),
                    executor.submit(() -> run(action, ready, start)));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            int successes = 0;
            for (Future<Boolean> future : futures) {
                if (future.get(10, TimeUnit.SECONDS)) successes++;
            }
            return successes;
        } finally {
            executor.shutdownNow();
        }
    }

    private boolean run(Runnable action, CountDownLatch ready, CountDownLatch start) {
        ready.countDown();
        try {
            start.await();
            action.run();
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    private OtpVerification saveIssued(String email, OtpPurpose purpose, String otp) {
        LocalDateTime now = LocalDateTime.now();
        OtpVerification verification = new OtpVerification();
        verification.setNormalizedEmail(email);
        verification.setPurpose(purpose);
        verification.setOtpHash(passwordEncoder.encode(otp));
        verification.setStatus(OtpVerificationStatus.ISSUED);
        verification.setIssuedAt(now);
        verification.setExpiresAt(now.plusMinutes(5));
        verification.setLastRequestedAt(now);
        verification.setRequestWindowStartedAt(now);
        verification.setRequestCount(1);
        verification.setCreatedAt(now);
        verification.setUpdatedAt(now);
        return repository.saveAndFlush(verification);
    }
}
