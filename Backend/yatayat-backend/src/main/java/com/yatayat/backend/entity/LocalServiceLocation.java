package com.yatayat.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "local_service_locations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_local_service_location_run",
                columnNames = "local_service_run_id"
        )
)
public class LocalServiceLocation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "local_service_run_id",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(name = "fk_local_service_location_run")
    )
    private LocalServiceRun run;

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
    public LocalServiceRun getRun() { return run; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public Double getAccuracy() { return accuracy; }
    public Double getSpeed() { return speed; }
    public Double getHeading() { return heading; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }

    public void setId(Long id) { this.id = id; }
    public void setRun(LocalServiceRun run) { this.run = run; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public void setAccuracy(Double accuracy) { this.accuracy = accuracy; }
    public void setSpeed(Double speed) { this.speed = speed; }
    public void setHeading(Double heading) { this.heading = heading; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
