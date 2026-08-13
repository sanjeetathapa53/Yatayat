package com.yatayat.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DriverProfileUpdateRequest(
        @NotBlank(message = "Full name is required.")
        @Size(max = 120, message = "Full name must be 120 characters or fewer.")
        String fullName,

        @NotBlank(message = "Phone number is required.")
        @Pattern(
                regexp = "^[0-9+()\\-\\s]{7,20}$",
                message = "Enter a valid phone number."
        )
        String phone,

        @NotBlank(message = "Permanent address is required.")
        @Size(max = 300, message = "Permanent address must be 300 characters or fewer.")
        String permanentAddress,

        @NotBlank(message = "Current address is required.")
        @Size(max = 300, message = "Current address must be 300 characters or fewer.")
        String currentAddress,

        @NotBlank(message = "Emergency contact name is required.")
        @Size(max = 120, message = "Emergency contact name must be 120 characters or fewer.")
        String emergencyContactName,

        @NotBlank(message = "Emergency contact phone is required.")
        @Pattern(
                regexp = "^[0-9+()\\-\\s]{7,20}$",
                message = "Enter a valid emergency contact phone number."
        )
        String emergencyContactPhone,

        @Size(max = 200, message = "Preferred operating area must be 200 characters or fewer.")
        String preferredOperatingArea
) {
}