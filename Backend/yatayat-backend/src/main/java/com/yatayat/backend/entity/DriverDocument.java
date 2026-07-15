package com.yatayat.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "driver_documents")
public class DriverDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_profile_id")
    private DriverProfile driverProfile;

    @Enumerated(EnumType.STRING)
    private DriverDocumentType documentType;

    private String originalFileName;

    private String storedFileName;

    private String filePath;

    @Enumerated(EnumType.STRING)
    private DocumentVerificationStatus verificationStatus =
            DocumentVerificationStatus.PENDING;

    private LocalDateTime uploadedAt;

    @PrePersist
    public void onCreate() {
        uploadedAt = LocalDateTime.now();

        if (verificationStatus == null) {
            verificationStatus = DocumentVerificationStatus.PENDING;
        }
    }

    public DriverDocument() {}

    public Long getId() {
        return id;
    }

    public DriverProfile getDriverProfile() {
        return driverProfile;
    }

    public DriverDocumentType getDocumentType() {
        return documentType;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public String getStoredFileName() {
        return storedFileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public DocumentVerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setDriverProfile(DriverProfile driverProfile) {
        this.driverProfile = driverProfile;
    }

    public void setDocumentType(DriverDocumentType documentType) {
        this.documentType = documentType;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public void setStoredFileName(String storedFileName) {
        this.storedFileName = storedFileName;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public void setVerificationStatus(
            DocumentVerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
}