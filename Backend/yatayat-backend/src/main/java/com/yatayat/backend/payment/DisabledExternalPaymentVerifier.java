package com.yatayat.backend.payment;

import com.yatayat.backend.entity.PaymentMethod;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DisabledExternalPaymentVerifier implements ExternalPaymentVerifier {
    @Override
    public boolean isConfigured(PaymentMethod provider) {
        return false;
    }

    @Override
    public VerificationResult verify(PaymentMethod provider, String paymentReference,
                                     String providerTransactionId, String bookingReference,
                                     BigDecimal expectedAmount) {
        return VerificationResult.unavailable(
                provider.name() + " backend verification is not configured.");
    }
}
