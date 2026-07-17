package com.yatayat.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public record SeatAvailabilityResponse(
        Long tripId, Integer busCapacity,
        List<String> availableSeats, List<String> heldSeats, List<String> confirmedSeats,
        List<String> ownHeldSeats, LocalDateTime ownHoldExpiresAt
) {}
