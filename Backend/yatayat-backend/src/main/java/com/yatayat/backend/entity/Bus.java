package com.yatayat.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "buses",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_bus_number",
                        columnNames = "bus_number"
                ),
                @UniqueConstraint(
                        name = "uk_bus_permit_number",
                        columnNames = "permit_number"
                )
        }
)
public class Bus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "bus_number",
            nullable = false,
            unique = true,
            length = 50
    )
    private String busNumber;

    @Column(nullable = false, length = 120)
    private String busName;

    @Column(length = 100)
    private String model;

    private Integer manufactureYear;

    @Column(nullable = false)
    private Integer seatCapacity;

    @Column(nullable = false, length = 50)
    private String busType;

    @Column(length = 50)
    private String fuelType;

    @Column(
            name = "permit_number",
            unique = true,
            length = 100
    )
    private String permitNumber;

    private LocalDate permitExpiryDate;

    private LocalDate insuranceExpiryDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BusStatus status = BusStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "operator_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_bus_transport_operator"
            )
    )
    private TransportOperator operator;

    @Column(name = "operator_name", nullable = false, length = 150)
    private String operatorName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "driver_profile_id",
            foreignKey = @ForeignKey(
                    name = "fk_bus_assigned_driver"
            )
    )
    private DriverProfile assignedDriver;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public Bus() {
    }

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();

        createdAt = now;
        updatedAt = now;

        if (status == null) {
            status = BusStatus.PENDING;
        }
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean isAssigned() {
        return assignedDriver != null;
    }

    public boolean isPermitExpired() {
        return permitExpiryDate != null
                && permitExpiryDate.isBefore(LocalDate.now());
    }

    public boolean isInsuranceExpired() {
        return insuranceExpiryDate != null
                && insuranceExpiryDate.isBefore(LocalDate.now());
    }

    public boolean isOperationallyValid() {
        return status == BusStatus.ACTIVE
                && !isPermitExpired()
                && !isInsuranceExpired();
    }

    public Long getId() {
        return id;
    }

    public String getBusNumber() {
        return busNumber;
    }

    public String getBusName() {
        return busName;
    }

    public String getModel() {
        return model;
    }

    public Integer getManufactureYear() {
        return manufactureYear;
    }

    public Integer getSeatCapacity() {
        return seatCapacity;
    }

    public String getBusType() {
        return busType;
    }

    public String getFuelType() {
        return fuelType;
    }

    public String getPermitNumber() {
        return permitNumber;
    }

    public LocalDate getPermitExpiryDate() {
        return permitExpiryDate;
    }

    public LocalDate getInsuranceExpiryDate() {
        return insuranceExpiryDate;
    }

    public BusStatus getStatus() {
        return status;
    }

    public TransportOperator getOperator() {
        return operator;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public DriverProfile getAssignedDriver() {
        return assignedDriver;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setBusNumber(String busNumber) {
        this.busNumber = normalize(busNumber);
    }

    public void setBusName(String busName) {
        this.busName = clean(busName);
    }

    public void setModel(String model) {
        this.model = clean(model);
    }

    public void setManufactureYear(Integer manufactureYear) {
        this.manufactureYear = manufactureYear;
    }

    public void setSeatCapacity(Integer seatCapacity) {
        this.seatCapacity = seatCapacity;
    }

    public void setBusType(String busType) {
        this.busType = clean(busType);
    }

    public void setFuelType(String fuelType) {
        this.fuelType = clean(fuelType);
    }

    public void setPermitNumber(String permitNumber) {
        this.permitNumber = normalize(permitNumber);
    }

    public void setPermitExpiryDate(
            LocalDate permitExpiryDate
    ) {
        this.permitExpiryDate = permitExpiryDate;
    }

    public void setInsuranceExpiryDate(
            LocalDate insuranceExpiryDate
    ) {
        this.insuranceExpiryDate = insuranceExpiryDate;
    }

    public void setStatus(BusStatus status) {
        this.status = status;
    }

    public void setOperator(
            TransportOperator operator
    ) {
        this.operator = operator;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = clean(operatorName);
    }

    public void setAssignedDriver(
            DriverProfile assignedDriver
    ) {
        this.assignedDriver = assignedDriver;
    }

    public void setCreatedAt(
            LocalDateTime createdAt
    ) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(
            LocalDateTime updatedAt
    ) {
        this.updatedAt = updatedAt;
    }

    private String clean(String value) {
        return value == null
                ? null
                : value.trim();
    }

    private String normalize(String value) {
        return value == null
                ? null
                : value.trim().toUpperCase();
    }
}
