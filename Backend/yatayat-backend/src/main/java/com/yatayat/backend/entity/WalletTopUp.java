package com.yatayat.backend.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "wallet_topups", indexes = {
        @Index(name = "idx_wallet_topup_wallet_created", columnList = "wallet_id,created_at"),
        @Index(name = "idx_wallet_topup_passenger_created", columnList = "passenger_id,created_at")
})
public class WalletTopUp {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "passenger_id", nullable = false)
    private User passenger;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_transaction_id", unique = true)
    private WalletTransaction walletTransaction;

    @Column(name = "top_up_reference", nullable = false, length = 48, unique = true)
    private String topUpReference;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.INITIATED;

    @Column(name = "provider_payment_id", length = 100, unique = true)
    private String providerPaymentId;
    @Column(name = "provider_transaction_id", length = 100, unique = true)
    private String providerTransactionId;
    @Column(name = "provider_payment_url", length = 1000)
    private String providerPaymentUrl;
    private LocalDateTime providerExpiresAt;
    private LocalDateTime initiatedAt;
    private LocalDateTime verifiedAt;
    private LocalDateTime creditedAt;
    @Column(length = 500)
    private String failureReason;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) status = PaymentStatus.INITIATED;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Wallet getWallet() { return wallet; }
    public User getPassenger() { return passenger; }
    public WalletTransaction getWalletTransaction() { return walletTransaction; }
    public String getTopUpReference() { return topUpReference; }
    public BigDecimal getAmount() { return amount; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public PaymentStatus getStatus() { return status; }
    public String getProviderPaymentId() { return providerPaymentId; }
    public String getProviderTransactionId() { return providerTransactionId; }
    public String getProviderPaymentUrl() { return providerPaymentUrl; }
    public LocalDateTime getProviderExpiresAt() { return providerExpiresAt; }
    public LocalDateTime getInitiatedAt() { return initiatedAt; }
    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public LocalDateTime getCreditedAt() { return creditedAt; }
    public String getFailureReason() { return failureReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setWallet(Wallet value) { wallet = value; }
    public void setPassenger(User value) { passenger = value; }
    public void setWalletTransaction(WalletTransaction value) { walletTransaction = value; }
    public void setTopUpReference(String value) { topUpReference = clean(value); }
    public void setAmount(BigDecimal value) { amount = value; }
    public void setPaymentMethod(PaymentMethod value) { paymentMethod = value; }
    public void setStatus(PaymentStatus value) { status = value; }
    public void setProviderPaymentId(String value) { providerPaymentId = clean(value); }
    public void setProviderTransactionId(String value) { providerTransactionId = clean(value); }
    public void setProviderPaymentUrl(String value) { providerPaymentUrl = clean(value); }
    public void setProviderExpiresAt(LocalDateTime value) { providerExpiresAt = value; }
    public void setInitiatedAt(LocalDateTime value) { initiatedAt = value; }
    public void setVerifiedAt(LocalDateTime value) { verifiedAt = value; }
    public void setCreditedAt(LocalDateTime value) { creditedAt = value; }
    public void setFailureReason(String value) { failureReason = clean(value); }
    private String clean(String value) { return value == null ? null : value.trim(); }
}
