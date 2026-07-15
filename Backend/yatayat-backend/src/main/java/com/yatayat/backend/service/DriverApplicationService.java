package com.yatayat.backend.service;

import com.yatayat.backend.entity.*;
import com.yatayat.backend.repository.DriverDocumentRepository;
import com.yatayat.backend.repository.DriverProfileRepository;
import com.yatayat.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DriverApplicationService {

    private final UserRepository userRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final DriverDocumentRepository driverDocumentRepository;
    private final DriverDocumentStorageService storageService;

    public DriverApplicationService(
            UserRepository userRepository,
            DriverProfileRepository driverProfileRepository,
            DriverDocumentRepository driverDocumentRepository,
            DriverDocumentStorageService storageService
    ) {
        this.userRepository = userRepository;
        this.driverProfileRepository = driverProfileRepository;
        this.driverDocumentRepository = driverDocumentRepository;
        this.storageService = storageService;
    }

    @Transactional
    public Map<String, Object> submitApplication(
            String email,
            String dateOfBirth,
            String permanentAddress,
            String currentAddress,
            String emergencyContactName,
            String emergencyContactPhone,
            String citizenshipNumber,
            String licenseNumber,
            String licenseCategory,
            String licenseIssueDate,
            String licenseExpiryDate,
            Integer yearsOfExperience,
            String preferredOperatingArea,
            String applicationNote,
            MultipartFile profilePhoto,
            MultipartFile citizenshipFront,
            MultipartFile citizenshipBack,
            MultipartFile licenseFront,
            MultipartFile licenseBack
    ) {
        User user = userRepository.findByEmail(email).orElseThrow(
                () -> new IllegalArgumentException("Driver account not found")
        );

        if (!"DRIVER".equalsIgnoreCase(user.getRole())) {
            throw new IllegalArgumentException(
                    "Only driver accounts can submit this application"
            );
        }

        DriverProfile profile = driverProfileRepository.findByUser(user)
                .orElseGet(() -> new DriverProfile(user));

        if (profile.getVerificationStatus()
                == DriverVerificationStatus.APPROVED) {
            throw new IllegalArgumentException(
                    "This driver application is already approved"
            );
        }

        validateDuplicateDetails(profile, licenseNumber, citizenshipNumber);
        validateDates(licenseIssueDate, licenseExpiryDate);

        profile.setDateOfBirth(LocalDate.parse(dateOfBirth));
        profile.setPermanentAddress(permanentAddress.trim());
        profile.setCurrentAddress(currentAddress.trim());
        profile.setEmergencyContactName(emergencyContactName.trim());
        profile.setEmergencyContactPhone(emergencyContactPhone.trim());

        profile.setCitizenshipNumber(citizenshipNumber.trim());
        profile.setLicenseNumber(licenseNumber.trim());
        profile.setLicenseCategory(licenseCategory.trim());
        profile.setLicenseIssueDate(LocalDate.parse(licenseIssueDate));
        profile.setLicenseExpiryDate(LocalDate.parse(licenseExpiryDate));
        profile.setYearsOfExperience(yearsOfExperience);
        profile.setPreferredOperatingArea(
                preferredOperatingArea == null
                        ? ""
                        : preferredOperatingArea.trim()
        );
        profile.setApplicationNote(
                applicationNote == null ? "" : applicationNote.trim()
        );

        profile.setVerificationStatus(DriverVerificationStatus.PENDING);
        profile.setRejectionReason(null);
        profile.setSubmittedAt(LocalDateTime.now());
        profile.setReviewedAt(null);
        profile.setApprovedAt(null);

        DriverProfile savedProfile =
                driverProfileRepository.save(profile);

        replaceDocuments(
                savedProfile,
                profilePhoto,
                citizenshipFront,
                citizenshipBack,
                licenseFront,
                licenseBack
        );

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put(
                "message",
                "Driver application submitted for admin review"
        );
        response.put("application", cleanProfile(savedProfile));

        return response;
    }

    public Map<String, Object> getApplicationStatus(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(
                () -> new IllegalArgumentException("User not found")
        );

        DriverProfile profile =
                driverProfileRepository.findByUser(user).orElse(null);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);

        if (profile == null) {
            response.put("status", "NOT_SUBMITTED");
            response.put(
                    "message",
                    "Driver application has not been submitted"
            );
            return response;
        }

        response.put(
                "status",
                profile.getVerificationStatus().name()
        );
        response.put("application", cleanProfile(profile));

        return response;
    }

    private void validateDuplicateDetails(
            DriverProfile currentProfile,
            String licenseNumber,
            String citizenshipNumber
    ) {
        driverProfileRepository.findByLicenseNumber(licenseNumber.trim())
                .ifPresent(existing -> {
                    if (
                            currentProfile.getId() == null ||
                                    !existing.getId().equals(currentProfile.getId())
                    ) {
                        throw new IllegalArgumentException(
                                "Licence number is already registered"
                        );
                    }
                });

        driverProfileRepository
                .findByCitizenshipNumber(citizenshipNumber.trim())
                .ifPresent(existing -> {
                    if (
                            currentProfile.getId() == null ||
                                    !existing.getId().equals(currentProfile.getId())
                    ) {
                        throw new IllegalArgumentException(
                                "Citizenship number is already registered"
                        );
                    }
                });
    }

    private void validateDates(
            String licenseIssueDate,
            String licenseExpiryDate
    ) {
        LocalDate issueDate = LocalDate.parse(licenseIssueDate);
        LocalDate expiryDate = LocalDate.parse(licenseExpiryDate);

        if (!expiryDate.isAfter(issueDate)) {
            throw new IllegalArgumentException(
                    "Licence expiry date must be after issue date"
            );
        }

        if (expiryDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "Driving licence has already expired"
            );
        }
    }

    private void replaceDocuments(
            DriverProfile profile,
            MultipartFile profilePhoto,
            MultipartFile citizenshipFront,
            MultipartFile citizenshipBack,
            MultipartFile licenseFront,
            MultipartFile licenseBack
    ) {
        List<DriverDocument> oldDocuments =
                driverDocumentRepository.findByDriverProfile(profile);

        driverDocumentRepository.deleteAll(oldDocuments);

        saveDocument(
                profile,
                DriverDocumentType.PROFILE_PHOTO,
                profilePhoto
        );

        saveDocument(
                profile,
                DriverDocumentType.CITIZENSHIP_FRONT,
                citizenshipFront
        );

        saveDocument(
                profile,
                DriverDocumentType.CITIZENSHIP_BACK,
                citizenshipBack
        );

        saveDocument(
                profile,
                DriverDocumentType.LICENSE_FRONT,
                licenseFront
        );

        saveDocument(
                profile,
                DriverDocumentType.LICENSE_BACK,
                licenseBack
        );
    }

    private void saveDocument(
            DriverProfile profile,
            DriverDocumentType documentType,
            MultipartFile file
    ) {
        DriverDocumentStorageService.StoredDriverFile storedFile =
                storageService.store(
                        file,
                        profile.getId(),
                        documentType
                );

        DriverDocument document = new DriverDocument();
        document.setDriverProfile(profile);
        document.setDocumentType(documentType);
        document.setOriginalFileName(
                storedFile.originalFileName()
        );
        document.setStoredFileName(
                storedFile.storedFileName()
        );
        document.setFilePath(storedFile.filePath());
        document.setVerificationStatus(
                DocumentVerificationStatus.PENDING
        );

        driverDocumentRepository.save(document);
    }

    public Map<String, Object> getDriverProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(
                        () -> new IllegalArgumentException("User not found")
                );

        if (!"DRIVER".equalsIgnoreCase(user.getRole())) {
            throw new IllegalArgumentException(
                    "This account is not a driver"
            );
        }

        DriverProfile profile =
                driverProfileRepository.findByUser(user).orElse(null);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);

        Map<String, Object> driver = new HashMap<>();
        driver.put("userId", user.getId());
        driver.put("fullName", user.getFullName());
        driver.put("email", user.getEmail());
        driver.put("phone", user.getPhone());
        driver.put("role", user.getRole());

        if (profile == null) {
            driver.put("applicationId", null);
            driver.put("verificationStatus", "NOT_SUBMITTED");
        } else {
            driver.put("applicationId", profile.getId());
            driver.put(
                    "verificationStatus",
                    profile.getVerificationStatus().name()
            );
            driver.put("dateOfBirth", profile.getDateOfBirth());
            driver.put(
                    "permanentAddress",
                    profile.getPermanentAddress()
            );
            driver.put(
                    "currentAddress",
                    profile.getCurrentAddress()
            );
            driver.put(
                    "emergencyContactName",
                    profile.getEmergencyContactName()
            );
            driver.put(
                    "emergencyContactPhone",
                    profile.getEmergencyContactPhone()
            );
            driver.put(
                    "citizenshipNumber",
                    profile.getCitizenshipNumber()
            );
            driver.put(
                    "licenseNumber",
                    profile.getLicenseNumber()
            );
            driver.put(
                    "licenseCategory",
                    profile.getLicenseCategory()
            );
            driver.put(
                    "licenseIssueDate",
                    profile.getLicenseIssueDate()
            );
            driver.put(
                    "licenseExpiryDate",
                    profile.getLicenseExpiryDate()
            );
            driver.put(
                    "yearsOfExperience",
                    profile.getYearsOfExperience()
            );
            driver.put(
                    "preferredOperatingArea",
                    profile.getPreferredOperatingArea()
            );
            driver.put(
                    "approvedAt",
                    profile.getApprovedAt()
            );
        }

        response.put("driver", driver);
        return response;
    }

    private Map<String, Object> cleanProfile(
            DriverProfile profile
    ) {
        Map<String, Object> data = new HashMap<>();

        data.put("id", profile.getId());
        data.put(
                "userId",
                profile.getUser().getId()
        );
        data.put(
                "fullName",
                profile.getUser().getFullName()
        );
        data.put(
                "email",
                profile.getUser().getEmail()
        );
        data.put(
                "phone",
                profile.getUser().getPhone()
        );

        data.put(
                "dateOfBirth",
                profile.getDateOfBirth()
        );
        data.put(
                "permanentAddress",
                profile.getPermanentAddress()
        );
        data.put(
                "currentAddress",
                profile.getCurrentAddress()
        );
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
                "licenseNumber",
                profile.getLicenseNumber()
        );
        data.put(
                "licenseCategory",
                profile.getLicenseCategory()
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
                "yearsOfExperience",
                profile.getYearsOfExperience()
        );
        data.put(
                "preferredOperatingArea",
                profile.getPreferredOperatingArea()
        );
        data.put(
                "applicationNote",
                profile.getApplicationNote()
        );

        data.put(
                "verificationStatus",
                profile.getVerificationStatus()
        );
        data.put(
                "rejectionReason",
                profile.getRejectionReason()
        );
        data.put(
                "submittedAt",
                profile.getSubmittedAt()
        );
        data.put(
                "reviewedAt",
                profile.getReviewedAt()
        );
        data.put(
                "approvedAt",
                profile.getApprovedAt()
        );

        return data;
    }
}