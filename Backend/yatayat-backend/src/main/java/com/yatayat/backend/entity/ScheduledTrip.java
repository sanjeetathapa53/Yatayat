package com.yatayat.backend.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "scheduled_trips",
        indexes = {
                @Index(name = "idx_trip_operator_departure", columnList = "operator_id,departure_at"),
                @Index(name = "idx_trip_bus_window", columnList = "bus_id,departure_at,estimated_arrival_at"),
                @Index(name = "idx_trip_driver_window", columnList = "driver_profile_id,departure_at,estimated_arrival_at"),
                @Index(name = "idx_trip_route_departure_status", columnList = "route_id,departure_at,status"),
                @Index(name = "idx_trip_status_departure", columnList = "status,departure_at")
        }
)
public class ScheduledTrip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operator_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_trip_transport_operator"))
    private TransportOperator operator;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "route_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_trip_route"))
    private Route route;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bus_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_trip_bus"))
    private Bus bus;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "driver_profile_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_trip_driver_profile"))
    private DriverProfile driver;

    @Column(name = "departure_at", nullable = false)
    private LocalDateTime departureAt;

    @Column(name = "estimated_arrival_at", nullable = false)
    private LocalDateTime estimatedArrivalAt;

    private LocalDateTime actualDepartureAt;
    private LocalDateTime actualArrivalAt;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal fare;

    @Column(nullable = false)
    private Integer seatCapacitySnapshot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TripStatus status = TripStatus.SCHEDULED;

    @Column(length = 1000)
    private String boardingNotes;

    @Column(length = 1000)
    private String cancellationReason;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) status = TripStatus.SCHEDULED;
    }

    @PreUpdate
    public void onUpdate() { updatedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public TransportOperator getOperator() { return operator; }
    public Route getRoute() { return route; }
    public Bus getBus() { return bus; }
    public DriverProfile getDriver() { return driver; }
    public LocalDateTime getDepartureAt() { return departureAt; }
    public LocalDateTime getEstimatedArrivalAt() { return estimatedArrivalAt; }
    public LocalDateTime getActualDepartureAt() { return actualDepartureAt; }
    public LocalDateTime getActualArrivalAt() { return actualArrivalAt; }
    public BigDecimal getFare() { return fare; }
    public Integer getSeatCapacitySnapshot() { return seatCapacitySnapshot; }
    public TripStatus getStatus() { return status; }
    public String getBoardingNotes() { return boardingNotes; }
    public String getCancellationReason() { return cancellationReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }

    public void setId(Long id) { this.id = id; }
    public void setOperator(TransportOperator operator) { this.operator = operator; }
    public void setRoute(Route route) { this.route = route; }
    public void setBus(Bus bus) { this.bus = bus; }
    public void setDriver(DriverProfile driver) { this.driver = driver; }
    public void setDepartureAt(LocalDateTime departureAt) { this.departureAt = departureAt; }
    public void setEstimatedArrivalAt(LocalDateTime value) { this.estimatedArrivalAt = value; }
    public void setActualDepartureAt(LocalDateTime value) { this.actualDepartureAt = value; }
    public void setActualArrivalAt(LocalDateTime value) { this.actualArrivalAt = value; }
    public void setFare(BigDecimal fare) { this.fare = fare; }
    public void setSeatCapacitySnapshot(Integer value) { this.seatCapacitySnapshot = value; }
    public void setStatus(TripStatus status) { this.status = status; }
    public void setBoardingNotes(String value) { this.boardingNotes = clean(value); }
    public void setCancellationReason(String value) { this.cancellationReason = clean(value); }
    public void setCreatedAt(LocalDateTime value) { this.createdAt = value; }
    public void setUpdatedAt(LocalDateTime value) { this.updatedAt = value; }

    private String clean(String value) { return value == null ? null : value.trim(); }
}
