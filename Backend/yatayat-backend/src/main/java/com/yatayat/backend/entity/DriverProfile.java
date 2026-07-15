package com.yatayat.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "driver_profiles",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_driver_profile_user",
                        columnNames = "user_id"
                ),
                @UniqueConstraint(
                        name = "uk_driver_license_number",
                        columnNames = "license_number"
                ),
                @UniqueConstraint(
                        name = "uk_driver_citizenship_number",
                        columnNames = "citizenship_number"
                )
        }
)
public class DriverProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    private LocalDate dateOfBirth;

    @Column(length = 300)
    private String permanentAddress;

    @Column(length = 300)
    private String currentAddress;

    @Column(length = 120)
    private String emergencyContactName;

    @Column(length = 30)
    private String emergencyContactPhone;

    @Column(nullable = false, unique = true, length = 80)
    private String citizenshipNumber;

    @Column(nullable = false, unique = true, length = 80)
    private String licenseNumber;

    @Column(nullable = false, length = 40)
    private String licenseCategory;

    private LocalDate licenseIssueDate;

    @Column(nullable = false)
    private LocalDate licenseExpiryDate;

    private Integer yearsOfExperience;

    @Column(length = 200)
    private String preferredOperatingArea;

    @Column(length = 1000)
    private String applicationNote;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DriverVerificationStatus verificationStatus =
            DriverVerificationStatus.DRAFT;

    @Column(length = 1000)
    private String rejectionReason;

    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    private LocalDateTime approvedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public DriverProfile() {
    }

    public DriverProfile(User user) {
        this.user = user;
        this.verificationStatus = DriverVerificationStatus.DRAFT;
    }

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;

        if (verificationStatus == null) {
            verificationStatus = DriverVerificationStatus.DRAFT;
        }
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean isApproved() {
        return verificationStatus == DriverVerificationStatus.APPROVED;
    }

    public boolean isLicenseExpired() {
        return licenseExpiryDate != null
                && licenseExpiryDate.isBefore(LocalDate.now());
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public String getPermanentAddress() {
        return permanentAddress;
    }

    public String getCurrentAddress() {
        return currentAddress;
    }

    public String getEmergencyContactName() {
        return emergencyContactName;
    }

    public String getEmergencyContactPhone() {
        return emergencyContactPhone;
    }

    public String getCitizenshipNumber() {
        return citizenshipNumber;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public String getLicenseCategory() {
        return licenseCategory;
    }

    public LocalDate getLicenseIssueDate() {
        return licenseIssueDate;
    }

    public LocalDate getLicenseExpiryDate() {
        return licenseExpiryDate;
    }

    public Integer getYearsOfExperience() {
        return yearsOfExperience;
    }

    public String getPreferredOperatingArea() {
        return preferredOperatingArea;
    }

    public String getApplicationNote() {
        return applicationNote;
    }

    public DriverVerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
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

    public void setUser(User user) {
        this.user = user;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public void setPermanentAddress(String permanentAddress) {
        this.permanentAddress = permanentAddress;
    }

    public void setCurrentAddress(String currentAddress) {
        this.currentAddress = currentAddress;
    }

    public void setEmergencyContactName(String emergencyContactName) {
        this.emergencyContactName = emergencyContactName;
    }

    public void setEmergencyContactPhone(String emergencyContactPhone) {
        this.emergencyContactPhone = emergencyContactPhone;
    }

    public void setCitizenshipNumber(String citizenshipNumber) {
        this.citizenshipNumber = citizenshipNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    public void setLicenseCategory(String licenseCategory) {
        this.licenseCategory = licenseCategory;
    }

    public void setLicenseIssueDate(LocalDate licenseIssueDate) {
        this.licenseIssueDate = licenseIssueDate;
    }

    public void setLicenseExpiryDate(LocalDate licenseExpiryDate) {
        this.licenseExpiryDate = licenseExpiryDate;
    }

    public void setYearsOfExperience(Integer yearsOfExperience) {
        this.yearsOfExperience = yearsOfExperience;
    }

    public void setPreferredOperatingArea(String preferredOperatingArea) {
        this.preferredOperatingArea = preferredOperatingArea;
    }

    public void setApplicationNote(String applicationNote) {
        this.applicationNote = applicationNote;
    }

    public void setVerificationStatus(
            DriverVerificationStatus verificationStatus
    ) {
        this.verificationStatus = verificationStatus;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
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