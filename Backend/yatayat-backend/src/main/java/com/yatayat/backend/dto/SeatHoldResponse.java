package com.yatayat.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public record SeatHoldResponse(Long tripId, List<String> seatNumbers, LocalDateTime holdExpiresAt) {}
