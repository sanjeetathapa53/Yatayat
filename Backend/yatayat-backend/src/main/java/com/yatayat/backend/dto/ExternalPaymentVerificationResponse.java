package com.yatayat.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ExternalPaymentVerificationResponse(
        String bookingReference,
        String bookingStatus,
        String provider,
        String paymentReference,
        String providerTransactionId,
        String paymentStatus,
        BigDecimal paidAmount,
        LocalDateTime verifiedAt,
        String ticketNumber
) {}
