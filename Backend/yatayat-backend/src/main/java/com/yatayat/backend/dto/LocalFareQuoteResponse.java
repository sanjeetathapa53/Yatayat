package com.yatayat.backend.dto;

import java.math.BigDecimal;

public record LocalFareQuoteResponse(
        Long routeId,
        String routeCode,
        String routeName,
        Long boardingStopId,
        String boardingStopName,
        Integer boardingStopOrder,
        Long destinationStopId,
        String destinationStopName,
        Integer destinationStopOrder,
        BigDecimal fare
) {}
