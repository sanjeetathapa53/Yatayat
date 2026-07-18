package com.yatayat.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public record DriverTicketValidationResponse(
        String result,
        String message,
        String ticketNumber,
        String passengerName,
        RouteSummary route,
        List<String> seatNumbers,
        LocalDateTime boardedAt,
        String scheduledTripReference
) {
    public record RouteSummary(String origin, String destination) {
    }
}
