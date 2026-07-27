package com.yatayat.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LocalFarePassResponse(
        String passNumber,
        Long routeId,
        String routeCode,
        String routeName,
        Long boardingStopId,
        String boardingStopName,
        Long destinationStopId,
        String destinationStopName,
        BigDecimal fare,
        String status,
        LocalDateTime issuedAt,
        LocalDateTime validFrom,
        LocalDateTime validUntil,
        LocalDateTime usedAt,
        String qrPayload
) {}
