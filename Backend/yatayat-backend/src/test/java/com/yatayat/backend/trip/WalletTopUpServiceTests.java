package com.yatayat.backend.trip;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yatayat.backend.dto.*;
import com.yatayat.backend.entity.*;
import com.yatayat.backend.payment.*;
import com.yatayat.backend.repository.*;
import com.yatayat.backend.service.WalletTopUpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WalletTopUpServiceTests {
    private UserRepository users;
    private WalletRepository wallets;
    private WalletTopUpRepository topUps;
    private WalletTransactionRepository transactions;
    private KhaltiGateway khalti;
    private EsewaGateway esewa;
    private EsewaSignatureService signatures;
    private KhaltiProperties khaltiProperties;
    private EsewaProperties esewaProperties;
    private WalletTopUpService service;
    private User passenger;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        wallets = mock(WalletRepository.class);
        topUps = mock(WalletTopUpRepository.class);
        transactions = mock(WalletTransactionRepository.class);
        khalti = mock(KhaltiGateway.class);
        esewa = mock(EsewaGateway.class);
        signatures = new EsewaSignatureService();
        khaltiProperties = new KhaltiProperties();
        khaltiProperties.setEnabled(true);
        esewaProperties = new EsewaProperties();
        esewaProperties.setEnabled(true);
        esewaProperties.setProductCode("EPAYTEST");
        esewaProperties.setSecretKey("unit-test-secret");
        service = new WalletTopUpService(
                users, wallets, topUps, transactions, khalti, khaltiProperties,
                esewa, esewaProperties, signatures, new ObjectMapper(),
                new BigDecimal("100.00"), new BigDecimal("50000.00"));
        passenger = new User("Passenger", "passenger@example.com", "9800000000",
                "encoded", "PASSENGER");
        passenger.setId(7L);
        wallet = new Wallet(passenger);
        wallet.setId(3L);
        wallet.setWalletPin("encoded-pin");
        wallet.setBalance(100.0);
        when(users.findByEmailIgnoreCase(passenger.getEmail())).thenReturn(Optional.of(passenger));
        when(wallets.findWithLockByUser(passenger)).thenReturn(Optional.of(wallet));
        when(topUps.existsByTopUpReference(anyString())).thenReturn(false);
        when(topUps.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactions.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void inactiveWalletCannotInitiateTopUpAndPinIsNeverRequested() {
        wallet.setWalletPin(null);
        assertStatus(409, () -> service.initiateEsewa(
                passenger.getEmail(), new CreateWalletTopUpRequest(new BigDecimal("500"))));
        verifyNoInteractions(esewa);
    }

    @Test
    void rejectsMissingZeroNegativeBelowMinimumAboveMaximumAndExcessScale() {
        assertStatus(400, () -> service.initiateEsewa(passenger.getEmail(), null));
        for (String amount : List.of("0", "-1", "99.99", "50000.01", "100.001")) {
            assertStatus(400, () -> service.initiateEsewa(
                    passenger.getEmail(), new CreateWalletTopUpRequest(new BigDecimal(amount))));
        }
        verify(topUps, never()).saveAndFlush(any());
    }

    @Test
    void khaltiInitiationUsesBackendAmountAndStoresProviderAttempt() {
        when(khalti.initiate(any())).thenReturn(new KhaltiGateway.InitiationResult(
                "pidx-topup", "https://test-pay.khalti.com/?pidx=pidx-topup",
                null, 1800L));
        WalletTopUpInitiationResponse response = service.initiateKhalti(
                passenger.getEmail(), new CreateWalletTopUpRequest(new BigDecimal("500.00")));
        var request = org.mockito.ArgumentCaptor.forClass(KhaltiGateway.InitiationRequest.class);
        verify(khalti).initiate(request.capture());
        assertThat(request.getValue().amount()).isEqualTo(50000L);
        assertThat(request.getValue().returnUrl()).contains("/wallet/topup/khalti/callback");
        assertThat(response.redirectUrl()).startsWith("https://test-pay.khalti.com/");
        assertThat(response.amount()).isEqualByComparingTo("500.00");
    }

    @Test
    void esewaInitiationReturnsBackendSignedOfficialForm() {
        WalletTopUpInitiationResponse response = service.initiateEsewa(
                passenger.getEmail(), new CreateWalletTopUpRequest(new BigDecimal("500.00")));
        assertThat(response.formAction()).isEqualTo(esewaProperties.getFormUrl());
        assertThat(response.formFields()).containsEntry("total_amount", "500.00")
                .containsEntry("product_code", "EPAYTEST");
        String canonical = "total_amount=500.00,transaction_uuid="
                + response.formFields().get("transaction_uuid") + ",product_code=EPAYTEST";
        assertThat(signatures.verify(canonical, response.formFields().get("signature"),
                esewaProperties.getSecretKey())).isTrue();
    }

    @Test
    void repeatedInitiationReusesTheExistingProviderAttempt() {
        WalletTopUp existing = attempt(PaymentMethod.ESEWA, "existing-uuid");
        when(topUps.findFirstByWalletAndPaymentMethodAndAmountAndStatusOrderByCreatedAtDesc(
                wallet, PaymentMethod.ESEWA, new BigDecimal("500.00"), PaymentStatus.INITIATED))
                .thenReturn(Optional.of(existing));
        WalletTopUpInitiationResponse response = service.initiateEsewa(
                passenger.getEmail(), new CreateWalletTopUpRequest(new BigDecimal("500")));
        assertThat(response.topUpReference()).isEqualTo(existing.getTopUpReference());
        assertThat(response.formFields().get("transaction_uuid")).isEqualTo("existing-uuid");
        verify(topUps, never()).saveAndFlush(any());
    }

    @Test
    void successfulKhaltiTopUpCreditsAndRecordsExactlyOnce() {
        WalletTopUp topUp = attempt(PaymentMethod.KHALTI, "pidx-topup");
        when(topUps.findOwnedWithLock(topUp.getTopUpReference(), passenger.getId()))
                .thenReturn(Optional.of(topUp));
        when(khalti.lookup("pidx-topup")).thenReturn(new KhaltiGateway.LookupResult(
                "pidx-topup", 50000L, "Completed", "khalti-txn", false));

        WalletTopUpVerificationResponse first = service.verifyKhalti(
                passenger.getEmail(), topUp.getTopUpReference(),
                new KhaltiPaymentVerificationRequest("pidx-topup"));
        WalletTopUpVerificationResponse repeated = service.verifyKhalti(
                passenger.getEmail(), topUp.getTopUpReference(),
                new KhaltiPaymentVerificationRequest("pidx-topup"));

        assertThat(first.credited()).isTrue();
        assertThat(repeated.credited()).isTrue();
        assertThat(wallet.getBalance()).isEqualTo(600.0);
        verify(khalti, times(1)).lookup("pidx-topup");
        verify(transactions, times(1)).save(any(WalletTransaction.class));
    }

    @Test
    void successfulEsewaTopUpRequiresValidSignatureAndCreditsOnce() {
        WalletTopUp topUp = attempt(PaymentMethod.ESEWA, "uuid-topup");
        when(topUps.findOwnedWithLock(topUp.getTopUpReference(), passenger.getId()))
                .thenReturn(Optional.of(topUp));
        when(esewa.lookup("EPAYTEST", "500.00", "uuid-topup"))
                .thenReturn(new EsewaGateway.StatusResult(
                        "EPAYTEST", "uuid-topup", new BigDecimal("500.0"),
                        "COMPLETE", "esewa-ref"));

        WalletTopUpVerificationResponse result = service.verifyEsewa(
                passenger.getEmail(), topUp.getTopUpReference(),
                new EsewaPaymentVerificationRequest("corrupted-unsigned", callback(
                        "uuid-topup", "500.0", "COMPLETE", "unit-test-secret")));

        assertThat(result.credited()).isTrue();
        assertThat(result.providerTransactionId()).isEqualTo("esewa-ref");
        assertThat(wallet.getBalance()).isEqualTo(600.0);
        verify(transactions, times(1)).save(any(WalletTransaction.class));
    }

    @Test
    void invalidEsewaSignatureAndAmountMismatchNeverCredit() {
        WalletTopUp topUp = attempt(PaymentMethod.ESEWA, "uuid-topup");
        when(topUps.findOwnedWithLock(topUp.getTopUpReference(), passenger.getId()))
                .thenReturn(Optional.of(topUp));
        EsewaPaymentVerificationRequest invalidSignature = new EsewaPaymentVerificationRequest(
                "", callback("uuid-topup", "500.0", "COMPLETE", "wrong-secret"));
        assertStatus(409, () -> service.verifyEsewa(
                passenger.getEmail(), topUp.getTopUpReference(), invalidSignature));

        EsewaPaymentVerificationRequest wrongAmount = new EsewaPaymentVerificationRequest(
                "", callback("uuid-topup", "501.0", "COMPLETE", "unit-test-secret"));
        assertStatus(409, () -> service.verifyEsewa(
                passenger.getEmail(), topUp.getTopUpReference(), wrongAmount));
        verifyNoInteractions(esewa);
        verify(transactions, never()).save(any());
    }

    @Test
    void unsignedEsewaCallbackCannotCreditEvenWhenLookupSaysComplete() {
        WalletTopUp topUp = attempt(PaymentMethod.ESEWA, "uuid-topup");
        when(topUps.findOwnedWithLock(topUp.getTopUpReference(), passenger.getId()))
                .thenReturn(Optional.of(topUp));
        when(esewa.lookup("EPAYTEST", "500.00", "uuid-topup"))
                .thenReturn(new EsewaGateway.StatusResult(
                        "EPAYTEST", "uuid-topup", new BigDecimal("500"),
                        "COMPLETE", "esewa-ref"));
        assertStatus(409, () -> service.verifyEsewa(
                passenger.getEmail(), topUp.getTopUpReference(),
                new EsewaPaymentVerificationRequest("uuid-topup", "")));
        verify(transactions, never()).save(any());
    }

    @Test
    void pendingCancelledAndProviderMismatchDoNotCredit() {
        WalletTopUp khaltiAttempt = attempt(PaymentMethod.KHALTI, "pidx-topup");
        when(topUps.findOwnedWithLock(khaltiAttempt.getTopUpReference(), passenger.getId()))
                .thenReturn(Optional.of(khaltiAttempt));
        when(khalti.lookup("pidx-topup")).thenReturn(new KhaltiGateway.LookupResult(
                "pidx-topup", 50000L, "Pending", null, false));
        assertThat(service.verifyKhalti(passenger.getEmail(), khaltiAttempt.getTopUpReference(),
                new KhaltiPaymentVerificationRequest("pidx-topup")).paymentStatus())
                .isEqualTo("PENDING");
        khaltiAttempt.setStatus(PaymentStatus.INITIATED);
        when(khalti.lookup("pidx-topup")).thenReturn(new KhaltiGateway.LookupResult(
                "pidx-topup", 50000L, "User canceled", null, false));
        assertThat(service.verifyKhalti(passenger.getEmail(), khaltiAttempt.getTopUpReference(),
                new KhaltiPaymentVerificationRequest("pidx-topup")).paymentStatus())
                .isEqualTo("CANCELLED");
        khaltiAttempt.setStatus(PaymentStatus.INITIATED);
        when(khalti.lookup("pidx-topup")).thenReturn(new KhaltiGateway.LookupResult(
                "pidx-topup", 50000L, "Rejected", null, false));
        assertThat(service.verifyKhalti(passenger.getEmail(), khaltiAttempt.getTopUpReference(),
                new KhaltiPaymentVerificationRequest("pidx-topup")).paymentStatus())
                .isEqualTo("FAILED");
        verify(transactions, never()).save(any());

        WalletTopUp esewaAttempt = attempt(PaymentMethod.ESEWA, "uuid");
        when(topUps.findOwnedWithLock(esewaAttempt.getTopUpReference(), passenger.getId()))
                .thenReturn(Optional.of(esewaAttempt));
        assertStatus(409, () -> service.verifyKhalti(
                passenger.getEmail(), esewaAttempt.getTopUpReference(),
                new KhaltiPaymentVerificationRequest("uuid")));
    }

    @Test
    void khaltiAmountOrMissingProviderTransactionNeverCredits() {
        WalletTopUp topUp = attempt(PaymentMethod.KHALTI, "pidx-topup");
        when(topUps.findOwnedWithLock(topUp.getTopUpReference(), passenger.getId()))
                .thenReturn(Optional.of(topUp));
        when(khalti.lookup("pidx-topup")).thenReturn(new KhaltiGateway.LookupResult(
                "pidx-topup", 49999L, "Completed", "provider-id", false));
        assertStatus(409, () -> service.verifyKhalti(
                passenger.getEmail(), topUp.getTopUpReference(),
                new KhaltiPaymentVerificationRequest("pidx-topup")));
        when(khalti.lookup("pidx-topup")).thenReturn(new KhaltiGateway.LookupResult(
                "pidx-topup", 50000L, "Completed", "", false));
        assertStatus(409, () -> service.verifyKhalti(
                passenger.getEmail(), topUp.getTopUpReference(),
                new KhaltiPaymentVerificationRequest("pidx-topup")));
        verify(transactions, never()).save(any());
    }

    @Test
    void crossPassengerTopUpIsNotDisclosed() {
        User other = new User("Other", "other@example.com", "", "", "PASSENGER");
        other.setId(8L);
        when(users.findByEmailIgnoreCase(other.getEmail())).thenReturn(Optional.of(other));
        assertStatus(404, () -> service.verifyKhalti(
                other.getEmail(), "WTU-MISSING", new KhaltiPaymentVerificationRequest("pidx")));
    }

    private WalletTopUp attempt(PaymentMethod method, String providerId) {
        WalletTopUp topUp = new WalletTopUp();
        topUp.setWallet(wallet);
        topUp.setPassenger(passenger);
        topUp.setTopUpReference("WTU-20260724-ABCDEF123456");
        topUp.setAmount(new BigDecimal("500.00"));
        topUp.setPaymentMethod(method);
        topUp.setStatus(PaymentStatus.INITIATED);
        topUp.setProviderPaymentId(providerId);
        topUp.setInitiatedAt(LocalDateTime.now());
        topUp.setProviderExpiresAt(LocalDateTime.now().plusMinutes(20));
        return topUp;
    }

    private String callback(String uuid, String amount, String status, String secret) {
        String fields = "transaction_code,status,total_amount,transaction_uuid,product_code,signed_field_names";
        String canonical = "transaction_code=ref-1,status=" + status + ",total_amount=" + amount
                + ",transaction_uuid=" + uuid + ",product_code=EPAYTEST,signed_field_names=" + fields;
        String json = "{\"transaction_code\":\"ref-1\",\"status\":\"" + status
                + "\",\"total_amount\":\"" + amount + "\",\"transaction_uuid\":\"" + uuid
                + "\",\"product_code\":\"EPAYTEST\",\"signed_field_names\":\"" + fields
                + "\",\"signature\":\"" + signatures.sign(canonical, secret) + "\"}";
        return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    private void assertStatus(int status,
                              org.assertj.core.api.ThrowableAssert.ThrowingCallable callable) {
        assertThatThrownBy(callable).isInstanceOfSatisfying(
                ResponseStatusException.class,
                exception -> assertThat(exception.getStatusCode().value()).isEqualTo(status));
    }

}
