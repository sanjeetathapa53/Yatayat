package com.yatayat.backend.dto;

import jakarta.validation.constraints.NotNull;

public record LocalFareQuoteRequest(
        @NotNull Long routeId,
        @NotNull Long boardingStopId,
        @NotNull Long destinationStopId
) {}
