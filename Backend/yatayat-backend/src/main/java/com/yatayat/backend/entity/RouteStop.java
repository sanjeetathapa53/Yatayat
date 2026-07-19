package com.yatayat.backend.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "route_stops",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_route_stop_order", columnNames = {"route_id", "stop_order"}),
                @UniqueConstraint(name = "uk_route_stop_stop", columnNames = {"route_id", "bus_stop_id"})
        },
        indexes = {
                @Index(name = "idx_route_stop_route_order", columnList = "route_id, stop_order"),
                @Index(name = "idx_route_stop_bus_stop", columnList = "bus_stop_id")
        }
)
public class RouteStop {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bus_stop_id", nullable = false)
    private BusStop busStop;

    @Column(name = "stop_order", nullable = false)
    private Integer stopOrder;

    @Column(nullable = false)
    private Integer estimatedMinutesFromStart;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal cumulativeFare;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Route getRoute() { return route; }
    public BusStop getBusStop() { return busStop; }
    public Integer getStopOrder() { return stopOrder; }
    public Integer getEstimatedMinutesFromStart() { return estimatedMinutesFromStart; }
    public BigDecimal getCumulativeFare() { return cumulativeFare; }
    public boolean isActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setId(Long id) { this.id = id; }
    public void setRoute(Route route) { this.route = route; }
    public void setBusStop(BusStop busStop) { this.busStop = busStop; }
    public void setStopOrder(Integer stopOrder) { this.stopOrder = stopOrder; }
    public void setEstimatedMinutesFromStart(Integer estimatedMinutesFromStart) { this.estimatedMinutesFromStart = estimatedMinutesFromStart; }
    public void setCumulativeFare(BigDecimal cumulativeFare) { this.cumulativeFare = cumulativeFare; }
    public void setActive(boolean active) { this.active = active; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
