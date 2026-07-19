package com.yatayat.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(
        name = "local_service_runs",
        indexes = {
                @Index(name = "idx_local_run_operator_date", columnList = "operator_id,service_date"),
                @Index(name = "idx_local_run_bus_window", columnList = "bus_id,service_date,planned_start_time,planned_end_time"),
                @Index(name = "idx_local_run_driver_window", columnList = "driver_profile_id,service_date,planned_start_time,planned_end_time"),
                @Index(name = "idx_local_run_route_date_status", columnList = "route_id,service_date,status")
        }
)
public class LocalServiceRun {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operator_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_local_run_operator"))
    private TransportOperator operator;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "route_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_local_run_route"))
    private Route route;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bus_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_local_run_bus"))
    private Bus bus;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "driver_profile_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_local_run_driver"))
    private DriverProfile driver;

    @Column(nullable = false)
    private LocalDate serviceDate;

    @Column(nullable = false)
    private LocalTime plannedStartTime;

    @Column(nullable = false)
    private LocalTime plannedEndTime;

    private LocalDateTime actualStartedAt;
    private LocalDateTime actualCompletedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LocalServiceRunStatus status = LocalServiceRunStatus.PLANNED;

    @Column(length = 1000)
    private String notes;

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
        if (status == null) status = LocalServiceRunStatus.PLANNED;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public TransportOperator getOperator() { return operator; }
    public Route getRoute() { return route; }
    public Bus getBus() { return bus; }
    public DriverProfile getDriver() { return driver; }
    public LocalDate getServiceDate() { return serviceDate; }
    public LocalTime getPlannedStartTime() { return plannedStartTime; }
    public LocalTime getPlannedEndTime() { return plannedEndTime; }
    public LocalDateTime getActualStartedAt() { return actualStartedAt; }
    public LocalDateTime getActualCompletedAt() { return actualCompletedAt; }
    public LocalServiceRunStatus getStatus() { return status; }
    public String getNotes() { return notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }

    public void setId(Long id) { this.id = id; }
    public void setOperator(TransportOperator operator) { this.operator = operator; }
    public void setRoute(Route route) { this.route = route; }
    public void setBus(Bus bus) { this.bus = bus; }
    public void setDriver(DriverProfile driver) { this.driver = driver; }
    public void setServiceDate(LocalDate serviceDate) { this.serviceDate = serviceDate; }
    public void setPlannedStartTime(LocalTime plannedStartTime) { this.plannedStartTime = plannedStartTime; }
    public void setPlannedEndTime(LocalTime plannedEndTime) { this.plannedEndTime = plannedEndTime; }
    public void setActualStartedAt(LocalDateTime actualStartedAt) { this.actualStartedAt = actualStartedAt; }
    public void setActualCompletedAt(LocalDateTime actualCompletedAt) { this.actualCompletedAt = actualCompletedAt; }
    public void setStatus(LocalServiceRunStatus status) { this.status = status; }
    public void setNotes(String notes) { this.notes = clean(notes); }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    private String clean(String value) {
        return value == null ? null : value.trim();
    }
}
