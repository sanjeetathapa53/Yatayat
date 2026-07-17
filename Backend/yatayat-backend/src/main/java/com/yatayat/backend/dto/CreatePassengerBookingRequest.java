package com.yatayat.backend.dto;

public record CreatePassengerBookingRequest(
        Long tripId, String passengerName, String passengerPhone, Integer numberOfSeats
) {}
