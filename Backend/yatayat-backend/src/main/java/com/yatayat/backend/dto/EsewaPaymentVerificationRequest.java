package com.yatayat.backend.dto;

public record EsewaPaymentVerificationRequest(
        String transactionUuid,
        String data
) {}
