package com.yatayat.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "otp_verifications",
        uniqueConstraints = @UniqueConstraint(name = "uk_otp_email_purpose",
                columnNames = {"normalized_email", "purpose"}),
        indexes = {
                @Index(name = "idx_otp_email_purpose", columnList = "normalized_email,purpose"),
                @Index(name = "idx_otp_expiry", columnList = "expires_at")
        })
public class OtpVerification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "normalized_email", nullable = false, length = 254)
    private String normalizedEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OtpPurpose purpose;

    @Column(name = "otp_hash", length = 100)
    private String otpHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private OtpVerificationStatus status;

    @Column(name = "issued_at")
    private LocalDateTime issuedAt;
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;
    @Column(name = "consumed_at")
    private LocalDateTime consumedAt;
    @Column(name = "last_requested_at")
    private LocalDateTime lastRequestedAt;
    @Column(name = "request_window_started_at")
    private LocalDateTime requestWindowStartedAt;
    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;
    @Column(name = "request_count", nullable = false)
    private int requestCount;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    @Version
    private Long version;

    public Long getId() { return id; }
    public String getNormalizedEmail() { return normalizedEmail; }
    public void setNormalizedEmail(String normalizedEmail) { this.normalizedEmail = normalizedEmail; }
    public OtpPurpose getPurpose() { return purpose; }
    public void setPurpose(OtpPurpose purpose) { this.purpose = purpose; }
    public String getOtpHash() { return otpHash; }
    public void setOtpHash(String otpHash) { this.otpHash = otpHash; }
    public OtpVerificationStatus getStatus() { return status; }
    public void setStatus(OtpVerificationStatus status) { this.status = status; }
    public LocalDateTime getIssuedAt() { return issuedAt; }
    public void setIssuedAt(LocalDateTime issuedAt) { this.issuedAt = issuedAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }
    public LocalDateTime getConsumedAt() { return consumedAt; }
    public void setConsumedAt(LocalDateTime consumedAt) { this.consumedAt = consumedAt; }
    public LocalDateTime getLastRequestedAt() { return lastRequestedAt; }
    public void setLastRequestedAt(LocalDateTime lastRequestedAt) { this.lastRequestedAt = lastRequestedAt; }
    public LocalDateTime getRequestWindowStartedAt() { return requestWindowStartedAt; }
    public void setRequestWindowStartedAt(LocalDateTime value) { this.requestWindowStartedAt = value; }
    public int getAttemptCount() { return attemptCount; }
    public void setAttemptCount(int attemptCount) { this.attemptCount = attemptCount; }
    public int getRequestCount() { return requestCount; }
    public void setRequestCount(int requestCount) { this.requestCount = requestCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Long getVersion() { return version; }
}
