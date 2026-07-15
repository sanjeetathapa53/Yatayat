package com.yatayat.backend.service;

import com.yatayat.backend.dto.OperatorApplicationRequest;
import com.yatayat.backend.entity.OperatorType;
import com.yatayat.backend.entity.OperatorVerificationStatus;
import com.yatayat.backend.entity.TransportOperator;
import com.yatayat.backend.entity.User;
import com.yatayat.backend.repository.TransportOperatorRepository;
import com.yatayat.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class OperatorApplicationService {

    private final UserRepository userRepository;
    private final TransportOperatorRepository operatorRepository;

    public OperatorApplicationService(
            UserRepository userRepository,
            TransportOperatorRepository operatorRepository
    ) {
        this.userRepository = userRepository;
        this.operatorRepository = operatorRepository;
    }

    @Transactional
    public Map<String, Object> submitApplication(
            OperatorApplicationRequest request
    ) {
        validateRequest(request);

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "Operator account not found"
                        )
                );

        if (!"OPERATOR".equalsIgnoreCase(user.getRole())) {
            throw new IllegalArgumentException(
                    "This account is not registered as an operator"
            );
        }

        if (operatorRepository.existsByUser(user)) {
            throw new IllegalArgumentException(
                    "Operator application already exists"
            );
        }

        operatorRepository
                .findByRegistrationNumberIgnoreCase(
                        request.getRegistrationNumber().trim()
                )
                .ifPresent(existing -> {
                    throw new IllegalArgumentException(
                            "Registration number already exists"
                    );
                });

        TransportOperator operator =
                new TransportOperator();

        operator.setUser(user);
        operator.setName(
                request.getOrganizationName().trim()
        );

        operator.setOperatorType(
                parseOperatorType(request.getOperatorType())
        );

        operator.setRegistrationNumber(
                request.getRegistrationNumber()
                        .trim()
                        .toUpperCase()
        );

        operator.setPermitNumber(
                normalizeNullable(
                        request.getPermitNumber()
                )
        );

        operator.setContactPerson(
                request.getContactPerson().trim()
        );

        /*
         * Operator email is taken from the authenticated user account.
         */
        operator.setEmail(
                user.getEmail().trim().toLowerCase()
        );

        operator.setPhone(request.getPhone().trim());
        operator.setAddress(request.getAddress().trim());

        operator.setVerificationStatus(
                OperatorVerificationStatus.PENDING
        );

        operator.setRejectionReason(null);
        operator.setApprovedAt(null);

        TransportOperator saved =
                operatorRepository.save(operator);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put(
                "message",
                "Operator application submitted successfully"
        );
        response.put("status", "PENDING");
        response.put("operator", toMap(saved));

        return response;
    }

    public Map<String, Object> getApplicationStatus(
            Long userId
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "Operator account not found"
                        )
                );

        if (!"OPERATOR".equalsIgnoreCase(user.getRole())) {
            throw new IllegalArgumentException(
                    "This account is not an operator"
            );
        }

        TransportOperator operator =
                operatorRepository.findByUser(user).orElse(null);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);

        if (operator == null) {
            response.put("status", "NOT_SUBMITTED");
            response.put(
                    "message",
                    "Operator application has not been submitted"
            );
            response.put("operator", null);

            return response;
        }

        response.put(
                "status",
                operator.getVerificationStatus().name()
        );

        response.put(
                "message",
                statusMessage(operator)
        );

        response.put("operator", toMap(operator));

        return response;
    }

    @Transactional
    public Map<String, Object> resubmitApplication(
            OperatorApplicationRequest request
    ) {
        validateRequest(request);

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "Operator account not found"
                        )
                );

        TransportOperator operator =
                operatorRepository.findByUser(user)
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "Operator application not found"
                                )
                        );

        if (
                operator.getVerificationStatus() !=
                        OperatorVerificationStatus.REJECTED
        ) {
            throw new IllegalArgumentException(
                    "Only a rejected application can be resubmitted"
            );
        }

        operatorRepository
                .findByRegistrationNumberIgnoreCase(
                        request.getRegistrationNumber().trim()
                )
                .ifPresent(existing -> {
                    if (!existing.getId().equals(operator.getId())) {
                        throw new IllegalArgumentException(
                                "Registration number already exists"
                        );
                    }
                });

        operator.setName(
                request.getOrganizationName().trim()
        );

        operator.setOperatorType(
                parseOperatorType(request.getOperatorType())
        );

        operator.setRegistrationNumber(
                request.getRegistrationNumber()
                        .trim()
                        .toUpperCase()
        );

        operator.setPermitNumber(
                normalizeNullable(
                        request.getPermitNumber()
                )
        );

        operator.setContactPerson(
                request.getContactPerson().trim()
        );

        operator.setPhone(request.getPhone().trim());
        operator.setAddress(request.getAddress().trim());

        operator.setVerificationStatus(
                OperatorVerificationStatus.PENDING
        );

        operator.setRejectionReason(null);
        operator.setApprovedAt(null);

        TransportOperator saved =
                operatorRepository.save(operator);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put(
                "message",
                "Operator application resubmitted successfully"
        );
        response.put("status", "PENDING");
        response.put("operator", toMap(saved));

        return response;
    }

    private void validateRequest(
            OperatorApplicationRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "Operator application is required"
            );
        }

        if (request.getUserId() == null) {
            throw new IllegalArgumentException(
                    "Operator user ID is required"
            );
        }

        if (isBlank(request.getOrganizationName())) {
            throw new IllegalArgumentException(
                    "Organization name is required"
            );
        }

        if (isBlank(request.getOperatorType())) {
            throw new IllegalArgumentException(
                    "Operator type is required"
            );
        }

        if (isBlank(request.getRegistrationNumber())) {
            throw new IllegalArgumentException(
                    "Registration number is required"
            );
        }

        if (isBlank(request.getContactPerson())) {
            throw new IllegalArgumentException(
                    "Contact person is required"
            );
        }

        if (isBlank(request.getPhone())) {
            throw new IllegalArgumentException(
                    "Phone number is required"
            );
        }

        if (isBlank(request.getAddress())) {
            throw new IllegalArgumentException(
                    "Address is required"
            );
        }

        parseOperatorType(request.getOperatorType());
    }

    private OperatorType parseOperatorType(String value) {
        try {
            return OperatorType.valueOf(
                    value.trim().toUpperCase()
            );
        } catch (Exception exception) {
            throw new IllegalArgumentException(
                    "Invalid operator type"
            );
        }
    }

    private String statusMessage(
            TransportOperator operator
    ) {
        return switch (
                operator.getVerificationStatus()
                ) {
            case APPROVED ->
                    "Your operator account has been approved";

            case REJECTED ->
                    operator.getRejectionReason() == null
                            ? "Your operator application was rejected"
                            : operator.getRejectionReason();

            case SUSPENDED ->
                    "Your operator account has been suspended";

            default ->
                    "Your operator application is awaiting admin review";
        };
    }

    private Map<String, Object> toMap(
            TransportOperator operator
    ) {
        Map<String, Object> data = new HashMap<>();

        data.put("id", operator.getId());
        data.put("userId", operator.getUser().getId());
        data.put("name", operator.getName());

        data.put(
                "operatorType",
                operator.getOperatorType().name()
        );

        data.put(
                "registrationNumber",
                operator.getRegistrationNumber()
        );

        data.put(
                "permitNumber",
                operator.getPermitNumber()
        );

        data.put(
                "contactPerson",
                operator.getContactPerson()
        );

        data.put("email", operator.getEmail());
        data.put("phone", operator.getPhone());
        data.put("address", operator.getAddress());

        data.put(
                "verificationStatus",
                operator.getVerificationStatus().name()
        );

        data.put(
                "rejectionReason",
                operator.getRejectionReason()
        );

        data.put("approvedAt", operator.getApprovedAt());
        data.put("createdAt", operator.getCreatedAt());
        data.put("updatedAt", operator.getUpdatedAt());

        return data;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim().toUpperCase();
    }
}