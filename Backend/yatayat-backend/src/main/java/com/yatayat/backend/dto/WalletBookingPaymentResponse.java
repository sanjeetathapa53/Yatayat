package com.yatayat.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record WalletBookingPaymentResponse(
        String bookingReference,
        String bookingStatus,
        String paymentStatus,
        String paymentMethod,
        BigDecimal paidAmount,
        LocalDateTime paidAt,
        String transactionReference,
        BigDecimal walletBalance,
        List<String> seatNumbers,
        String ticketNumber,
        String ticketEmailMessage
) {}
