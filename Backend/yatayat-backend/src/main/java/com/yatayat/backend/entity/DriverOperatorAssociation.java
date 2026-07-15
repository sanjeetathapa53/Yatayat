package com.yatayat.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "driver_operator_associations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_driver_operator_association",
                columnNames = {"driver_profile_id", "operator_id"}
        )
)
public class DriverOperatorAssociation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "driver_profile_id", nullable = false)
    private DriverProfile driver;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operator_id", nullable = false)
    private TransportOperator operator;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DriverOperatorAssociationStatus status =
            DriverOperatorAssociationStatus.PENDING;

    @Column(nullable = false)
    private LocalDateTime invitedAt;

    private LocalDateTime respondedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (invitedAt == null) invitedAt = now;
        if (status == null) status = DriverOperatorAssociationStatus.PENDING;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public DriverProfile getDriver() { return driver; }
    public TransportOperator getOperator() { return operator; }
    public DriverOperatorAssociationStatus getStatus() { return status; }
    public LocalDateTime getInvitedAt() { return invitedAt; }
    public LocalDateTime getRespondedAt() { return respondedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setDriver(DriverProfile driver) { this.driver = driver; }
    public void setOperator(TransportOperator operator) { this.operator = operator; }
    public void setStatus(DriverOperatorAssociationStatus status) { this.status = status; }
    public void setInvitedAt(LocalDateTime invitedAt) { this.invitedAt = invitedAt; }
    public void setRespondedAt(LocalDateTime respondedAt) { this.respondedAt = respondedAt; }
}
