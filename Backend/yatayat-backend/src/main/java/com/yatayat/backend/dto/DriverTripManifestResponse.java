package com.yatayat.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public record DriverTripManifestResponse(
        TripSummary trip,
        BoardingSummary summary,
        List<PassengerRow> passengers
) {
    public record TripSummary(
            String scheduledTripReference,
            String origin,
            String destination,
            String busName,
            String busNumber,
            LocalDateTime departureAt,
            String status
    ) {
    }

    public record BoardingSummary(
            int totalConfirmedPassengers,
            int boardedPassengers,
            int notYetBoardedPassengers
    ) {
    }

    public record PassengerRow(
            String passengerName,
            String passengerPhone,
            String bookingReference,
            String ticketNumber,
            List<String> seats,
            String ticketStatus,
            LocalDateTime boardedAt
    ) {
    }
}
