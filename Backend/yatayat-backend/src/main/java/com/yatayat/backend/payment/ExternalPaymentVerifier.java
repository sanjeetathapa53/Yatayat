package com.yatayat.backend.payment;

import com.yatayat.backend.entity.PaymentMethod;

import java.math.BigDecimal;

public interface ExternalPaymentVerifier {
    boolean isConfigured(PaymentMethod provider);

    VerificationResult verify(
            PaymentMethod provider,
            String paymentReference,
            String providerTransactionId,
            String bookingReference,
            BigDecimal expectedAmount
    );

    record VerificationResult(boolean verified, BigDecimal verifiedAmount, String failureReason) {
        public static VerificationResult unavailable(String reason) {
            return new VerificationResult(false, null, reason);
        }
    }
}
