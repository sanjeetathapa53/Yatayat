package com.yatayat.backend.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "routes",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_route_code",
                columnNames = "code"
        )
)
public class Route {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(nullable = false, length = 160)
    private String origin;

    @Column(nullable = false, length = 160)
    private String destination;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal distanceKm;

    @Column(nullable = false)
    private Integer estimatedDurationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private TripType tripType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RouteStatus status = RouteStatus.ACTIVE;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) status = RouteStatus.ACTIVE;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getOrigin() { return origin; }
    public String getDestination() { return destination; }
    public BigDecimal getDistanceKm() { return distanceKm; }
    public Integer getEstimatedDurationMinutes() { return estimatedDurationMinutes; }
    public TripType getTripType() { return tripType == null ? TripType.OUT_OF_VALLEY : tripType; }
    public RouteStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setId(Long id) { this.id = id; }
    public void setCode(String code) { this.code = normalize(code); }
    public void setName(String name) { this.name = clean(name); }
    public void setOrigin(String origin) { this.origin = clean(origin); }
    public void setDestination(String destination) { this.destination = clean(destination); }
    public void setDistanceKm(BigDecimal distanceKm) { this.distanceKm = distanceKm; }
    public void setEstimatedDurationMinutes(Integer estimatedDurationMinutes) { this.estimatedDurationMinutes = estimatedDurationMinutes; }
    public void setTripType(TripType tripType) { this.tripType = tripType; }
    public void setStatus(RouteStatus status) { this.status = status; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    private String clean(String value) {
        return value == null ? null : value.trim();
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }
}
