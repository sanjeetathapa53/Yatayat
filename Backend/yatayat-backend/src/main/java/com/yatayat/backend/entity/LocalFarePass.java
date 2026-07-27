package com.yatayat.backend.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "local_fare_passes",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_local_fare_pass_number", columnNames = "pass_number"),
                @UniqueConstraint(name = "uk_local_fare_pass_token", columnNames = "qr_token_hash"),
                @UniqueConstraint(name = "uk_local_fare_pass_wallet_tx", columnNames = "wallet_transaction_id")
        },
        indexes = {
                @Index(name = "idx_local_fare_pass_owner_status", columnList = "passenger_id,status,valid_until"),
                @Index(name = "idx_local_fare_pass_route_status", columnList = "route_id,status,valid_until")
        })
public class LocalFarePass {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pass_number", nullable = false, length = 48)
    private String passNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "passenger_id", nullable = false)
    private User passenger;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "boarding_stop_id", nullable = false)
    private BusStop boardingStop;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destination_stop_id", nullable = false)
    private BusStop destinationStop;

    @Column(nullable = false)
    private Integer boardingStopOrder;

    @Column(nullable = false)
    private Integer destinationStopOrder;

    @Column(nullable = false, length = 160)
    private String boardingStopName;

    @Column(nullable = false, length = 160)
    private String destinationStopName;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal fare;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LocalFarePassStatus status = LocalFarePassStatus.VALID;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_transaction_id", nullable = false)
    private WalletTransaction walletTransaction;

    @Column(nullable = false)
    private LocalDateTime issuedAt;

    @Column(nullable = false)
    private LocalDateTime validFrom;

    @Column(nullable = false)
    private LocalDateTime validUntil;

    @Column(name = "qr_token_hash", nullable = false, length = 64)
    private String qrTokenHash;

    private LocalDateTime usedAt;
    private LocalDateTime cancelledAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validated_by_driver_profile_id")
    private DriverProfile validatedByDriverProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validated_local_service_run_id")
    private LocalServiceRun validatedLocalServiceRun;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    @PrePersist
    void create() {
        LocalDateTime now = LocalDateTime.now();
        if (issuedAt == null) issuedAt = now;
        if (validFrom == null) validFrom = issuedAt;
        if (validUntil == null) validUntil = issuedAt.plusHours(24);
        if (status == null) status = LocalFarePassStatus.VALID;
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void update() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getPassNumber() { return passNumber; }
    public User getPassenger() { return passenger; }
    public Route getRoute() { return route; }
    public BusStop getBoardingStop() { return boardingStop; }
    public BusStop getDestinationStop() { return destinationStop; }
    public Integer getBoardingStopOrder() { return boardingStopOrder; }
    public Integer getDestinationStopOrder() { return destinationStopOrder; }
    public String getBoardingStopName() { return boardingStopName; }
    public String getDestinationStopName() { return destinationStopName; }
    public BigDecimal getFare() { return fare; }
    public LocalFarePassStatus getStatus() { return status; }
    public WalletTransaction getWalletTransaction() { return walletTransaction; }
    public LocalDateTime getIssuedAt() { return issuedAt; }
    public LocalDateTime getValidFrom() { return validFrom; }
    public LocalDateTime getValidUntil() { return validUntil; }
    public String getQrTokenHash() { return qrTokenHash; }
    public LocalDateTime getUsedAt() { return usedAt; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public DriverProfile getValidatedByDriverProfile() { return validatedByDriverProfile; }
    public LocalServiceRun getValidatedLocalServiceRun() { return validatedLocalServiceRun; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }
    public void setId(Long id) { this.id = id; }
    public void setPassNumber(String passNumber) { this.passNumber = passNumber; }
    public void setPassenger(User passenger) { this.passenger = passenger; }
    public void setRoute(Route route) { this.route = route; }
    public void setBoardingStop(BusStop boardingStop) { this.boardingStop = boardingStop; }
    public void setDestinationStop(BusStop destinationStop) { this.destinationStop = destinationStop; }
    public void setBoardingStopOrder(Integer boardingStopOrder) { this.boardingStopOrder = boardingStopOrder; }
    public void setDestinationStopOrder(Integer destinationStopOrder) { this.destinationStopOrder = destinationStopOrder; }
    public void setBoardingStopName(String boardingStopName) { this.boardingStopName = boardingStopName; }
    public void setDestinationStopName(String destinationStopName) { this.destinationStopName = destinationStopName; }
    public void setFare(BigDecimal fare) { this.fare = fare; }
    public void setStatus(LocalFarePassStatus status) { this.status = status; }
    public void setWalletTransaction(WalletTransaction walletTransaction) { this.walletTransaction = walletTransaction; }
    public void setIssuedAt(LocalDateTime issuedAt) { this.issuedAt = issuedAt; }
    public void setValidFrom(LocalDateTime validFrom) { this.validFrom = validFrom; }
    public void setValidUntil(LocalDateTime validUntil) { this.validUntil = validUntil; }
    public void setQrTokenHash(String qrTokenHash) { this.qrTokenHash = qrTokenHash; }
    public void setUsedAt(LocalDateTime usedAt) { this.usedAt = usedAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
    public void setValidatedByDriverProfile(DriverProfile value) { this.validatedByDriverProfile = value; }
    public void setValidatedLocalServiceRun(LocalServiceRun value) { this.validatedLocalServiceRun = value; }
}
