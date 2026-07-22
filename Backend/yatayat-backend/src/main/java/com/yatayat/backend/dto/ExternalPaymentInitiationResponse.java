package com.yatayat.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ExternalPaymentInitiationResponse(
        String bookingReference,
        String provider,
        String paymentReference,
        BigDecimal amount,
        String paymentStatus,
        LocalDateTime initiatedAt,
        boolean providerConfigured,
        String redirectUrl,
        String message
) {}
