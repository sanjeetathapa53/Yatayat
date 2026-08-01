package com.yatayat.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "driver_notifications",
        uniqueConstraints = @UniqueConstraint(name = "uk_driver_notification_event",
                columnNames = {"driver_profile_id", "notification_type", "event_key"}),
        indexes = {
                @Index(name = "idx_driver_notification_driver_created", columnList = "driver_profile_id,created_at"),
                @Index(name = "idx_driver_notification_driver_read", columnList = "driver_profile_id,read_at")
        })
public class DriverNotification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "driver_profile_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_driver_notification_driver"))
    private DriverProfile driver;
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 50)
    private DriverNotificationType type;
    @Column(nullable = false, length = 160) private String title;
    @Column(nullable = false, length = 1000) private String message;
    @Column(name = "related_entity_type", length = 50) private String relatedEntityType;
    @Column(name = "related_entity_id", length = 100) private String relatedEntityId;
    @Column(name = "event_key", nullable = false, length = 80) private String eventKey;
    @Column(name = "read_at") private LocalDateTime readAt;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;

    @PrePersist void onCreate() { if (createdAt == null) createdAt = LocalDateTime.now(); }
    public Long getId() { return id; }
    public DriverProfile getDriver() { return driver; }
    public void setDriver(DriverProfile driver) { this.driver = driver; }
    public DriverNotificationType getType() { return type; }
    public void setType(DriverNotificationType type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getRelatedEntityType() { return relatedEntityType; }
    public void setRelatedEntityType(String value) { relatedEntityType = value; }
    public String getRelatedEntityId() { return relatedEntityId; }
    public void setRelatedEntityId(String value) { relatedEntityId = value; }
    public String getEventKey() { return eventKey; }
    public void setEventKey(String eventKey) { this.eventKey = eventKey; }
    public LocalDateTime getReadAt() { return readAt; }
    public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
