package com.yatayat.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "transport_operators",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_operator_registration_number",
                        columnNames = "registration_number"
                ),
                @UniqueConstraint(
                        name = "uk_operator_email",
                        columnNames = "email"
                )
        }
)
public class TransportOperator {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(
                    name = "fk_operator_user"
            )
    )
    private User user;

    @Column(nullable = false, length = 180)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private OperatorType operatorType;

    @Column(
            name = "registration_number",
            nullable = false,
            unique = true,
            length = 100
    )
    private String registrationNumber;

    @Column(length = 120)
    private String permitNumber;

    @Column(nullable = false, length = 120)
    private String contactPerson;

    @Column(nullable = false, unique = true, length = 160)
    private String email;

    @Column(nullable = false, length = 30)
    private String phone;

    @Column(nullable = false, length = 300)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OperatorVerificationStatus verificationStatus =
            OperatorVerificationStatus.PENDING;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @Column(length = 1000)
    private String rejectionReason;

    private LocalDateTime approvedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public TransportOperator() {
    }

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();

        createdAt = now;
        updatedAt = now;

        if (verificationStatus == null) {
            verificationStatus =
                    OperatorVerificationStatus.PENDING;
        }
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean isApproved() {
        return verificationStatus ==
                OperatorVerificationStatus.APPROVED;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public OperatorType getOperatorType() {
        return operatorType;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public String getPermitNumber() {
        return permitNumber;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getAddress() {
        return address;
    }

    public OperatorVerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
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

    public void setName(String name) {
        this.name = name;
    }

    public void setOperatorType(OperatorType operatorType) {
        this.operatorType = operatorType;
    }

    public void setRegistrationNumber(
            String registrationNumber
    ) {
        this.registrationNumber = registrationNumber;
    }

    public void setPermitNumber(String permitNumber) {
        this.permitNumber = permitNumber;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setVerificationStatus(
            OperatorVerificationStatus verificationStatus
    ) {
        this.verificationStatus = verificationStatus;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}