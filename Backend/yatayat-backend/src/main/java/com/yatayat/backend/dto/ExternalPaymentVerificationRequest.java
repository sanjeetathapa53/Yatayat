package com.yatayat.backend.dto;

public record ExternalPaymentVerificationRequest(
        String paymentReference,
        String providerTransactionId
) {}
