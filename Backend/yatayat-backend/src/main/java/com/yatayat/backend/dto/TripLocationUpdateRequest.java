package com.yatayat.backend.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record TripLocationUpdateRequest(
        @NotNull(message = "Latitude is required.")
        @DecimalMin(value = "-90", message = "Latitude must be between -90 and 90.")
        @DecimalMax(value = "90", message = "Latitude must be between -90 and 90.")
        Double latitude,

        @NotNull(message = "Longitude is required.")
        @DecimalMin(value = "-180", message = "Longitude must be between -180 and 180.")
        @DecimalMax(value = "180", message = "Longitude must be between -180 and 180.")
        Double longitude,

        @DecimalMin(value = "0", message = "Accuracy must not be negative.")
        Double accuracy,

        @DecimalMin(value = "0", message = "Speed must not be negative.")
        Double speed,

        @DecimalMin(value = "0", message = "Heading must be between 0 and 360.")
        @DecimalMax(value = "360", message = "Heading must be between 0 and 360.")
        Double heading
) {
}
