package com.yatayat.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PassengerTripDetailsResponse(
        Long tripId, Long routeId, String routeCode, String routeName,
        String origin, String destination, LocalDateTime departureAt,
        LocalDateTime estimatedArrivalAt, BigDecimal fare, Integer seatCapacity,
        String status, String operatorName, String busNumber, String busName,
        Integer estimatedDurationMinutes, String tripType, String boardingNotes
) {}
