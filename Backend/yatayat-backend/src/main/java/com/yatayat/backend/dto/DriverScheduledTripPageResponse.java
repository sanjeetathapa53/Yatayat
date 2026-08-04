package com.yatayat.backend.dto;

import java.util.List;

public record DriverScheduledTripPageResponse(
        List<DriverScheduledTripResponse> content,
        int page, int size, long totalElements, int totalPages,
        boolean first, boolean last
) {}
