package com.yatayat.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public record EsewaPaymentInitiationResponse(
        String bookingReference,
        String provider,
        String paymentReference,
        BigDecimal amount,
        String paymentStatus,
        LocalDateTime initiatedAt,
        boolean providerConfigured,
        String formAction,
        Map<String, String> formFields,
        String message
) {}
