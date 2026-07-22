package com.yatayat.backend.dto;

public record AssociatedBusResponse(
        Long busId,
        String busNumber,
        String busName
) {
}
