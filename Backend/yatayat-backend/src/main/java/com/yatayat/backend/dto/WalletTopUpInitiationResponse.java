package com.yatayat.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public record WalletTopUpInitiationResponse(
        String topUpReference,
        String provider,
        BigDecimal amount,
        String paymentStatus,
        LocalDateTime initiatedAt,
        LocalDateTime expiresAt,
        String redirectUrl,
        String formAction,
        Map<String, String> formFields
) {}
