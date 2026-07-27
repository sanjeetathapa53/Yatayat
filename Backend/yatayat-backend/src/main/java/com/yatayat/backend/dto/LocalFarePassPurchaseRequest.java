package com.yatayat.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record LocalFarePassPurchaseRequest(
        @NotNull Long routeId,
        @NotNull Long boardingStopId,
        @NotNull Long destinationStopId,
        @NotBlank(message = "Wallet PIN is required.")
        @Pattern(regexp = "\\d{4}", message = "Wallet PIN must be 4 digits.")
        String walletPin
) {}
