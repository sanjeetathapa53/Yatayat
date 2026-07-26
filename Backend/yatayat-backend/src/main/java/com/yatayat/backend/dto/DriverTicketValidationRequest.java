package com.yatayat.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DriverTicketValidationRequest(
        @NotBlank(message = "QR payload is required.")
        @Size(max = 2048, message = "QR payload is too large.")
        String qrPayload
) {
}
