package com.yatayat.backend.dto;

import java.time.LocalDateTime;

public record DriverNotificationResponse(
        Long id, String type, String title, String message,
        String relatedEntityType, String relatedEntityId,
        boolean read, LocalDateTime readAt, LocalDateTime createdAt) {}
