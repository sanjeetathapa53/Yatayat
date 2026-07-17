package com.yatayat.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PassengerBookingSummaryResponse(
        String bookingReference, String bookingStatus, Long tripId,
        String routeCode, String routeName, String tripType, String origin, String destination,
        LocalDateTime departureAt, LocalDateTime estimatedArrivalAt,
        String operatorName, String busNumber, Integer numberOfSeats,
        BigDecimal farePerSeat, BigDecimal totalFare,
        LocalDateTime bookedAt, LocalDateTime cancelledAt
) {}
