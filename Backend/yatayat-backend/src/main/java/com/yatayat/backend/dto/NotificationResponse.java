package com.yatayat.backend.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record NotificationResponse(
        Long id,
        String type,
        String referenceId,
        Map<String, String> metadata,
        LocalDateTime createdAt,
        boolean read
) {}
