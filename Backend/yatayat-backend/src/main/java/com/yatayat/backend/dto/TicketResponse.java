package com.yatayat.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record TicketResponse(
        String ticketNumber,
        String bookingReference,
        String ticketStatus,
        String passengerName,
        String origin,
        String destination,
        String operatorName,
        String busName,
        String busNumber,
        LocalDateTime travelDate,
        LocalDateTime departureAt,
        String boardingPoint,
        String dropOffPoint,
        List<String> seatNumbers,
        BigDecimal totalFare,
        String paymentMethod,
        String paymentReference,
        LocalDateTime issuedAt,
        LocalDateTime validFrom,
        LocalDateTime validUntil,
        String qrPayload
) {}
