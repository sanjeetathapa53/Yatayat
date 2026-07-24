package com.yatayat.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WalletTopUpVerificationResponse(
        String topUpReference,
        String provider,
        BigDecimal amount,
        String paymentStatus,
        boolean credited,
        Double walletBalance,
        String providerTransactionId,
        LocalDateTime verifiedAt,
        String message
) {}
