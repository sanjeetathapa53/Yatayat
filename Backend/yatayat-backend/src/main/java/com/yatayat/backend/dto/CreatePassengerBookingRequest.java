package com.yatayat.backend.dto;

import java.util.List;

public record CreatePassengerBookingRequest(
        Long tripId, String passengerName, String passengerPhone,
        Integer numberOfSeats, List<String> seatNumbers
) {}
