package com.yatayat.backend.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments",
        uniqueConstraints = @UniqueConstraint(name = "uk_payment_booking_status",
                columnNames = {"booking_id", "status"}),
        indexes = {
                @Index(name = "idx_payment_passenger_created", columnList = "passenger_id,created_at"),
                @Index(name = "idx_payment_reference", columnList = "transaction_reference")
        })
public class Payment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_payment_booking"))
    private PassengerTripBooking booking;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "passenger_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_payment_passenger"))
    private User passenger;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "transaction_reference", nullable = false, length = 48, unique = true)
    private String transactionReference;

    private LocalDateTime paidAt;
    private LocalDateTime initiatedAt;
    private LocalDateTime verifiedAt;
    @Column(name = "provider_transaction_id", length = 100, unique = true)
    private String providerTransactionId;
    @Column(name = "failure_reason", length = 500)
    private String failureReason;
    @Column(nullable = false) private LocalDateTime createdAt;
    @Column(nullable = false) private LocalDateTime updatedAt;

    @PrePersist void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) status = PaymentStatus.PENDING;
    }

    @PreUpdate void onUpdate() { updatedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public PassengerTripBooking getBooking() { return booking; }
    public User getPassenger() { return passenger; }
    public BigDecimal getAmount() { return amount; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public PaymentStatus getStatus() { return status; }
    public String getTransactionReference() { return transactionReference; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public LocalDateTime getInitiatedAt() { return initiatedAt; }
    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public String getProviderTransactionId() { return providerTransactionId; }
    public String getFailureReason() { return failureReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setId(Long value) { id = value; }
    public void setBooking(PassengerTripBooking value) { booking = value; }
    public void setPassenger(User value) { passenger = value; }
    public void setAmount(BigDecimal value) { amount = value; }
    public void setPaymentMethod(PaymentMethod value) { paymentMethod = value; }
    public void setStatus(PaymentStatus value) { status = value; }
    public void setTransactionReference(String value) { transactionReference = value; }
    public void setPaidAt(LocalDateTime value) { paidAt = value; }
    public void setInitiatedAt(LocalDateTime value) { initiatedAt = value; }
    public void setVerifiedAt(LocalDateTime value) { verifiedAt = value; }
    public void setProviderTransactionId(String value) { providerTransactionId = clean(value); }
    public void setFailureReason(String value) { failureReason = clean(value); }
    private String clean(String value) { return value == null ? null : value.trim(); }
}
