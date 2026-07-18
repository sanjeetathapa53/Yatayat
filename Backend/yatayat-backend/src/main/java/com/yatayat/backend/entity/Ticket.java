package com.yatayat.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tickets",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_ticket_number", columnNames = "ticket_number"),
                @UniqueConstraint(name = "uk_ticket_booking", columnNames = "booking_id"),
                @UniqueConstraint(name = "uk_ticket_qr_token_hash", columnNames = "qr_token_hash")
        },
        indexes = {
                @Index(name = "idx_ticket_status_valid_until", columnList = "status,valid_until")
        })
public class Ticket {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_number", nullable = false, length = 40, unique = true)
    private String ticketNumber;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false, unique = true,
            foreignKey = @ForeignKey(name = "fk_ticket_booking"))
    private PassengerTripBooking booking;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketStatus status = TicketStatus.VALID;

    @Column(name = "qr_token_hash", nullable = false, length = 128, unique = true)
    private String qrTokenHash;

    @Column(nullable = false) private LocalDateTime issuedAt;
    @Column(nullable = false) private LocalDateTime validFrom;
    @Column(nullable = false) private LocalDateTime validUntil;
    private LocalDateTime usedAt;
    private LocalDateTime cancelledAt;
    @Enumerated(EnumType.STRING)
    @Column(name = "auto_email_status", nullable = false, length = 20)
    private TicketEmailStatus autoEmailStatus = TicketEmailStatus.PENDING;
    private LocalDateTime autoEmailSentAt;
    private LocalDateTime lastEmailAttemptAt;
    @Column(nullable = false) private LocalDateTime createdAt;
    @Column(nullable = false) private LocalDateTime updatedAt;

    @PrePersist void create() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (issuedAt == null) issuedAt = now;
        if (status == null) status = TicketStatus.VALID;
        if (autoEmailStatus == null) autoEmailStatus = TicketEmailStatus.PENDING;
    }

    @PreUpdate void update() { updatedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public String getTicketNumber() { return ticketNumber; }
    public PassengerTripBooking getBooking() { return booking; }
    public TicketStatus getStatus() { return status; }
    public String getQrTokenHash() { return qrTokenHash; }
    public LocalDateTime getIssuedAt() { return issuedAt; }
    public LocalDateTime getValidFrom() { return validFrom; }
    public LocalDateTime getValidUntil() { return validUntil; }
    public LocalDateTime getUsedAt() { return usedAt; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public TicketEmailStatus getAutoEmailStatus() { return autoEmailStatus; }
    public LocalDateTime getAutoEmailSentAt() { return autoEmailSentAt; }
    public LocalDateTime getLastEmailAttemptAt() { return lastEmailAttemptAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setId(Long id) { this.id = id; }
    public void setTicketNumber(String ticketNumber) { this.ticketNumber = ticketNumber; }
    public void setBooking(PassengerTripBooking booking) { this.booking = booking; }
    public void setStatus(TicketStatus status) { this.status = status; }
    public void setQrTokenHash(String qrTokenHash) { this.qrTokenHash = qrTokenHash; }
    public void setIssuedAt(LocalDateTime issuedAt) { this.issuedAt = issuedAt; }
    public void setValidFrom(LocalDateTime validFrom) { this.validFrom = validFrom; }
    public void setValidUntil(LocalDateTime validUntil) { this.validUntil = validUntil; }
    public void setUsedAt(LocalDateTime usedAt) { this.usedAt = usedAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
    public void setAutoEmailStatus(TicketEmailStatus autoEmailStatus) { this.autoEmailStatus = autoEmailStatus; }
    public void setAutoEmailSentAt(LocalDateTime autoEmailSentAt) { this.autoEmailSentAt = autoEmailSentAt; }
    public void setLastEmailAttemptAt(LocalDateTime lastEmailAttemptAt) { this.lastEmailAttemptAt = lastEmailAttemptAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
