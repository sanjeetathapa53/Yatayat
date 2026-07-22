package com.yatayat.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "trip_locations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_trip_location_trip",
                columnNames = "scheduled_trip_id"
        )
)
public class TripLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "scheduled_trip_id",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(name = "fk_trip_location_scheduled_trip")
    )
    private ScheduledTrip trip;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    private Double accuracy;
    private Double speed;
    private Double heading;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    @PrePersist
    @PreUpdate
    public void updateTimestamp() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public ScheduledTrip getTrip() { return trip; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public Double getAccuracy() { return accuracy; }
    public Double getSpeed() { return speed; }
    public Double getHeading() { return heading; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }

    public void setId(Long id) { this.id = id; }
    public void setTrip(ScheduledTrip trip) { this.trip = trip; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public void setAccuracy(Double accuracy) { this.accuracy = accuracy; }
    public void setSpeed(Double speed) { this.speed = speed; }
    public void setHeading(Double heading) { this.heading = heading; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
