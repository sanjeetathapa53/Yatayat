package com.yatayat.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DriverLocalFarePassValidationResponse(
        String result,
        String message,
        String passNumber,
        String passengerName,
        String boardingStopName,
        String destinationStopName,
        BigDecimal fare,
        LocalDateTime usedAt,
        Long localServiceRunId
) {}
