package com.yatayat.backend.service;

import com.yatayat.backend.entity.DocumentVerificationStatus;
import com.yatayat.backend.entity.DriverDocument;
import com.yatayat.backend.entity.DriverProfile;
import com.yatayat.backend.entity.DriverVerificationStatus;
import com.yatayat.backend.repository.DriverDocumentRepository;
import com.yatayat.backend.repository.DriverProfileRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminDriverService {

    private final DriverProfileRepository driverProfileRepository;
    private final DriverDocumentRepository driverDocumentRepository;

    public AdminDriverService(
            DriverProfileRepository driverProfileRepository,
            DriverDocumentRepository driverDocumentRepository
    ) {
        this.driverProfileRepository = driverProfileRepository;
        this.driverDocumentRepository = driverDocumentRepository;
    }

    public List<Map<String, Object>> getPendingApplications() {
        return driverProfileRepository
                .findByVerificationStatusOrderBySubmittedAtAsc(
                        DriverVerificationStatus.PENDING
                )
                .stream()
                .map(this::toSummary)
                .toList();
    }

    public Map<String, Object> getApplication(Long profileId) {
        DriverProfile profile = findProfile(profileId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("application", toDetails(profile));

        return response;
    }

    @Transactional
    public Map<String, Object> approveApplication(Long profileId) {
        DriverProfile profile = findProfile(profileId);

        if (profile.getVerificationStatus()
                == DriverVerificationStatus.APPROVED) {
            throw new IllegalArgumentException(
                    "Driver application is already approved"
            );
        }

        if (profile.isLicenseExpired()) {
            throw new IllegalArgumentException(
                    "Cannot approve a driver with an expired licence"
            );
        }

        profile.setVerificationStatus(
                DriverVerificationStatus.APPROVED
        );
        profile.setRejectionReason(null);
        profile.setReviewedAt(LocalDateTime.now());
        profile.setApprovedAt(LocalDateTime.now());

        DriverProfile savedProfile =
                driverProfileRepository.save(profile);

        List<DriverDocument> documents =
                driverDocumentRepository.findByDriverProfile(savedProfile);

        for (DriverDocument document : documents) {
            document.setVerificationStatus(
                    DocumentVerificationStatus.APPROVED
            );
        }

        driverDocumentRepository.saveAll(documents);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put(
                "message",
                "Driver application approved successfully"
        );
        response.put("application", toDetails(savedProfile));

        return response;
    }

    @Transactional
    public Map<String, Object> rejectApplication(
            Long profileId,
            String reason
    ) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException(
                    "Rejection reason is required"
            );
        }

        DriverProfile profile = findProfile(profileId);

        if (profile.getVerificationStatus()
                == DriverVerificationStatus.APPROVED) {
            throw new IllegalArgumentException(
                    "Approved driver application cannot be rejected directly"
            );
        }

        profile.setVerificationStatus(
                DriverVerificationStatus.REJECTED
        );
        profile.setRejectionReason(reason.trim());
        profile.setReviewedAt(LocalDateTime.now());
        profile.setApprovedAt(null);

        DriverProfile savedProfile =
                driverProfileRepository.save(profile);

        List<DriverDocument> documents =
                driverDocumentRepository.findByDriverProfile(savedProfile);

        for (DriverDocument document : documents) {
            document.setVerificationStatus(
                    DocumentVerificationStatus.REJECTED
            );
        }

        driverDocumentRepository.saveAll(documents);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Driver application rejected");
        response.put("application", toDetails(savedProfile));

        return response;
    }

    private DriverProfile findProfile(Long profileId) {
        return driverProfileRepository.findById(profileId)
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "Driver application not found"
                        )
                );
    }

    private Map<String, Object> toSummary(
            DriverProfile profile
    ) {
        Map<String, Object> data = new HashMap<>();

        data.put("id", profile.getId());
        data.put("userId", profile.getUser().getId());
        data.put("fullName", profile.getUser().getFullName());
        data.put("email", profile.getUser().getEmail());
        data.put("phone", profile.getUser().getPhone());

        data.put("licenseNumber", profile.getLicenseNumber());
        data.put("licenseCategory", profile.getLicenseCategory());
        data.put(
                "yearsOfExperience",
                profile.getYearsOfExperience()
        );
        data.put(
                "preferredOperatingArea",
                profile.getPreferredOperatingArea()
        );
        data.put(
                "verificationStatus",
                profile.getVerificationStatus().name()
        );
        data.put("submittedAt", profile.getSubmittedAt());

        return data;
    }

    private Map<String, Object> toDetails(
            DriverProfile profile
    ) {
        Map<String, Object> data = toSummary(profile);

        data.put("dateOfBirth", profile.getDateOfBirth());
        data.put(
                "permanentAddress",
                profile.getPermanentAddress()
        );
        data.put("currentAddress", profile.getCurrentAddress());

        data.put(
                "emergencyContactName",
                profile.getEmergencyContactName()
        );
        data.put(
                "emergencyContactPhone",
                profile.getEmergencyContactPhone()
        );

        data.put(
                "citizenshipNumber",
                profile.getCitizenshipNumber()
        );
        data.put(
                "licenseIssueDate",
                profile.getLicenseIssueDate()
        );
        data.put(
                "licenseExpiryDate",
                profile.getLicenseExpiryDate()
        );
        data.put(
                "applicationNote",
                profile.getApplicationNote()
        );

        data.put(
                "rejectionReason",
                profile.getRejectionReason()
        );
        data.put("reviewedAt", profile.getReviewedAt());
        data.put("approvedAt", profile.getApprovedAt());

        List<Map<String, Object>> documentData =
                driverDocumentRepository
                        .findByDriverProfile(profile)
                        .stream()
                        .map(document -> {
                            Map<String, Object> item =
                                    new HashMap<>();

                            item.put("id", document.getId());
                            item.put(
                                    "documentType",
                                    document.getDocumentType().name()
                            );
                            item.put(
                                    "originalFileName",
                                    document.getOriginalFileName()
                            );
                            item.put(
                                    "storedFileName",
                                    document.getStoredFileName()
                            );
                            item.put(
                                    "verificationStatus",
                                    document
                                            .getVerificationStatus()
                                            .name()
                            );
                            item.put(
                                    "uploadedAt",
                                    document.getUploadedAt()
                            );

                            return item;
                        })
                        .toList();

        data.put("documents", documentData);

        return data;
    }
}