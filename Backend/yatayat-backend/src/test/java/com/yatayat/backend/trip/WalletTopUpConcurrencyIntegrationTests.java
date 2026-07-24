package com.yatayat.backend.trip;

import com.yatayat.backend.dto.KhaltiPaymentVerificationRequest;
import com.yatayat.backend.entity.*;
import com.yatayat.backend.payment.KhaltiGateway;
import com.yatayat.backend.repository.*;
import com.yatayat.backend.service.WalletTopUpService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "yatayat.payments.khalti.enabled=true",
        "yatayat.payments.khalti.secret-key=integration-test-placeholder"
})
class WalletTopUpConcurrencyIntegrationTests {
    @Autowired private UserRepository users;
    @Autowired private WalletRepository wallets;
    @Autowired private WalletTopUpRepository topUps;
    @Autowired private WalletTransactionRepository transactions;
    @Autowired private WalletTopUpService service;
    @MockitoBean private KhaltiGateway khalti;

    @Test
    void concurrentSuccessfulCallbacksCreditAndRecordExactlyOnce() throws Exception {
        String unique = Long.toString(System.nanoTime());
        User passenger = users.save(new User(
                "Concurrent Passenger", "wallet-" + unique + "@example.com",
                "9800000000", "encoded", "PASSENGER"));
        Wallet wallet = new Wallet(passenger);
        wallet.setWalletPin("encoded-pin");
        wallet.setBalance(25.0);
        wallet = wallets.saveAndFlush(wallet);

        WalletTopUp topUp = new WalletTopUp();
        topUp.setWallet(wallet);
        topUp.setPassenger(passenger);
        topUp.setTopUpReference("WTU-CONCURRENT-" + unique);
        topUp.setAmount(new BigDecimal("500.00"));
        topUp.setPaymentMethod(PaymentMethod.KHALTI);
        topUp.setStatus(PaymentStatus.INITIATED);
        topUp.setProviderPaymentId("pidx-" + unique);
        topUp.setInitiatedAt(LocalDateTime.now());
        topUp.setProviderExpiresAt(LocalDateTime.now().plusMinutes(20));
        topUps.saveAndFlush(topUp);

        when(khalti.lookup("pidx-" + unique)).thenReturn(new KhaltiGateway.LookupResult(
                "pidx-" + unique, 50000L, "Completed", "provider-" + unique, false));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Boolean> callback = () -> {
            start.await(5, TimeUnit.SECONDS);
            return service.verifyKhalti(passenger.getEmail(), topUp.getTopUpReference(),
                    new KhaltiPaymentVerificationRequest("pidx-" + unique)).credited();
        };
        Future<Boolean> first = executor.submit(callback);
        Future<Boolean> second = executor.submit(callback);
        start.countDown();
        assertThat(first.get(15, TimeUnit.SECONDS)).isTrue();
        assertThat(second.get(15, TimeUnit.SECONDS)).isTrue();
        executor.shutdownNow();

        Wallet updated = wallets.findByUser(passenger).orElseThrow();
        List<WalletTransaction> ledger =
                transactions.findByWalletOrderByTransactionDateDesc(updated);
        assertThat(updated.getBalance()).isEqualTo(525.0);
        assertThat(ledger).singleElement().satisfies(transaction -> {
            assertThat(transaction.getType()).isEqualTo("TOPUP");
            assertThat(transaction.getAmount()).isEqualTo(500.0);
            assertThat(transaction.getPaymentMethod()).isEqualTo("KHALTI");
        });
        WalletTopUp settled = topUps.findByTopUpReferenceAndPassengerId(
                topUp.getTopUpReference(), passenger.getId()).orElseThrow();
        assertThat(settled.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(settled.getWalletTransaction()).isNotNull();
    }
}
