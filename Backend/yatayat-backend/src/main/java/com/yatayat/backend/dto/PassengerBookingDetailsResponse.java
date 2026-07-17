package com.yatayat.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PassengerBookingDetailsResponse(
        String bookingReference, String bookingStatus,
        String passengerName, String passengerPhone,
        Long tripId, String routeCode, String routeName, String tripType,
        String origin, String destination, String operatorName, String busNumber,
        LocalDateTime departureAt, LocalDateTime estimatedArrivalAt,
        Integer numberOfSeats, List<String> seatNumbers, BigDecimal farePerSeat, BigDecimal totalFare,
        LocalDateTime bookedAt, LocalDateTime cancelledAt, String boardingNotes
) {}
