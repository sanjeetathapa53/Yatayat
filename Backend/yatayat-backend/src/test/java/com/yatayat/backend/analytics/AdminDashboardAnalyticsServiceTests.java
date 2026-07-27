package com.yatayat.backend.analytics;

import com.yatayat.backend.entity.*;
import com.yatayat.backend.service.AdminDashboardAnalyticsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(AdminDashboardAnalyticsService.class)
class AdminDashboardAnalyticsServiceTests {
    @Autowired private TestEntityManager entities;
    @Autowired private AdminDashboardAnalyticsService analytics;

    @Test
    void summaryAndContinuousSevenDaySeriesUseDatabaseValues() {
        User passenger = new User("Passenger", "analytics-passenger@example.com",
                "9800000000", "encoded", "PASSENGER");
        passenger.setCreatedAt(LocalDateTime.now());
        entities.persistAndFlush(passenger);

        var response = analytics.dashboard("LAST_7_DAYS");

        assertThat(response.summary().totalUsers()).isEqualTo(1);
        assertThat(response.summary().totalPassengers()).isEqualTo(1);
        assertThat(response.summary().usersRegisteredToday()).isEqualTo(1);
        assertThat(response.userRegistrations()).hasSize(7);
        assertThat(response.bookings()).hasSize(7);
        assertThat(response.userRegistrations())
                .extracting(point -> point.date())
                .containsExactly(
                        LocalDate.now().minusDays(6), LocalDate.now().minusDays(5),
                        LocalDate.now().minusDays(4), LocalDate.now().minusDays(3),
                        LocalDate.now().minusDays(2), LocalDate.now().minusDays(1),
                        LocalDate.now());
    }

    @Test
    void walletRevenueIncludesOnlyVerifiedCreditedSuccess() {
        User passenger = entities.persist(new User("Passenger",
                "revenue-passenger@example.com", "9800000001", "encoded", "PASSENGER"));
        Wallet wallet = entities.persist(new Wallet(passenger));
        entities.persist(topUp(passenger, wallet, "TOP-SUCCESS",
                new BigDecimal("500.00"), PaymentStatus.SUCCESS, true));
        entities.persist(topUp(passenger, wallet, "TOP-PENDING",
                new BigDecimal("900.00"), PaymentStatus.PENDING, false));
        entities.persist(topUp(passenger, wallet, "TOP-FAILED",
                new BigDecimal("700.00"), PaymentStatus.FAILED, false));
        entities.flush();

        var summary = analytics.dashboard("LAST_7_DAYS").summary();

        assertThat(summary.verifiedWalletTopUpAmount())
                .isEqualByComparingTo("500.00");
        assertThat(summary.totalVerifiedPaymentAmount())
                .isEqualByComparingTo("500.00");
    }

    @Test
    void rejectsUnsupportedRange() {
        assertThatThrownBy(() -> analytics.dashboard("LAST_365_DAYS"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("LAST_7_DAYS");
    }

    private WalletTopUp topUp(User passenger, Wallet wallet, String reference,
                               BigDecimal amount, PaymentStatus status, boolean verified) {
        WalletTopUp topUp = new WalletTopUp();
        topUp.setPassenger(passenger);
        topUp.setWallet(wallet);
        topUp.setTopUpReference(reference);
        topUp.setAmount(amount);
        topUp.setPaymentMethod(PaymentMethod.KHALTI);
        topUp.setStatus(status);
        if (verified) {
            topUp.setVerifiedAt(LocalDateTime.now());
            topUp.setCreditedAt(LocalDateTime.now());
        }
        return topUp;
    }
}
