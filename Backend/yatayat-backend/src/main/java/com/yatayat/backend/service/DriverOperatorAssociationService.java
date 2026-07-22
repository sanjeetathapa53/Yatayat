package com.yatayat.backend.service;

import com.yatayat.backend.dto.*;
import com.yatayat.backend.entity.*;
import com.yatayat.backend.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DriverOperatorAssociationService {

    private final UserRepository userRepository;
    private final DriverProfileRepository driverRepository;
    private final TransportOperatorRepository operatorRepository;
    private final DriverOperatorAssociationRepository associationRepository;
    private final BusRepository busRepository;

    public DriverOperatorAssociationService(
            UserRepository userRepository,
            DriverProfileRepository driverRepository,
            TransportOperatorRepository operatorRepository,
            DriverOperatorAssociationRepository associationRepository,
            BusRepository busRepository
    ) {
        this.userRepository = userRepository;
        this.driverRepository = driverRepository;
        this.operatorRepository = operatorRepository;
        this.associationRepository = associationRepository;
        this.busRepository = busRepository;
    }

    public List<DriverOperatorAssociationResponse> getOperatorDrivers(String email) {
        TransportOperator operator = approvedOperator(email);
        return associationRepository.findByOperatorOrderByInvitedAtDesc(operator)
                .stream().map(this::toResponse).toList();
    }

    public List<EligibleDriverResponse> getEligibleDrivers(String email, String query) {
        TransportOperator operator = approvedOperator(email);
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase();

        return driverRepository.findByVerificationStatusOrderBySubmittedAtAsc(
                        DriverVerificationStatus.APPROVED
                ).stream()
                .filter(driver -> "DRIVER".equalsIgnoreCase(driver.getUser().getRole()))
                .filter(driver -> associationRepository.findByDriverAndStatus(
                        driver, DriverOperatorAssociationStatus.ACTIVE).isEmpty())
                .filter(driver -> associationRepository.findByDriverAndOperator(driver, operator)
                        .map(item -> item.getStatus() != DriverOperatorAssociationStatus.PENDING &&
                                item.getStatus() != DriverOperatorAssociationStatus.ACTIVE)
                        .orElse(true))
                .filter(driver -> matches(driver, normalizedQuery))
                .limit(25)
                .map(this::toEligibleResponse)
                .toList();
    }

    @Transactional
    public DriverOperatorAssociationResponse invite(
            String email,
            DriverInvitationRequest request
    ) {
        TransportOperator operator = approvedOperator(email);
        if (request == null || request.driverId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Driver ID is required");
        }

        DriverProfile driver = driverRepository.findLockedById(request.driverId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Driver not found"));
        requireApprovedDriver(driver);

        if (associationRepository.findByDriverAndStatus(
                driver, DriverOperatorAssociationStatus.ACTIVE).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Driver is already associated with an operator");
        }

        DriverOperatorAssociation association = associationRepository
                .findByDriverAndOperator(driver, operator).orElse(null);
        if (association != null && (association.getStatus() == DriverOperatorAssociationStatus.PENDING ||
                association.getStatus() == DriverOperatorAssociationStatus.ACTIVE)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "An invitation or active association already exists");
        }

        if (association == null) {
            association = new DriverOperatorAssociation();
            association.setDriver(driver);
            association.setOperator(operator);
        }
        association.setStatus(DriverOperatorAssociationStatus.PENDING);
        association.setInvitedAt(LocalDateTime.now());
        association.setRespondedAt(null);
        return toResponse(associationRepository.saveAndFlush(association));
    }

    public List<DriverOperatorAssociationResponse> getDriverInvitations(String email) {
        DriverProfile driver = approvedDriver(email);
        return associationRepository.findByDriverAndStatusOrderByInvitedAtDesc(
                        driver, DriverOperatorAssociationStatus.PENDING)
                .stream().map(this::toResponse).toList();
    }

    public DriverOperatorAssociationResponse getActiveAssociation(String email) {
        DriverProfile driver = approvedDriver(email);
        return associationRepository.findByDriverAndStatus(
                        driver, DriverOperatorAssociationStatus.ACTIVE)
                .map(this::toResponse).orElse(null);
    }

    @Transactional
    public DriverOperatorAssociationResponse accept(String email, Long associationId) {
        DriverProfile authenticated = approvedDriver(email);
        DriverProfile driver = driverRepository.findLockedById(authenticated.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Driver profile not found"));
        DriverOperatorAssociation invitation = ownedPendingInvitation(driver, associationId);

        if (associationRepository.findByDriverAndStatus(
                driver, DriverOperatorAssociationStatus.ACTIVE).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Driver already has an active operator association");
        }

        LocalDateTime now = LocalDateTime.now();
        invitation.setStatus(DriverOperatorAssociationStatus.ACTIVE);
        invitation.setRespondedAt(now);

        associationRepository.findByDriverAndStatusOrderByInvitedAtDesc(
                driver, DriverOperatorAssociationStatus.PENDING
        ).stream().filter(item -> !item.getId().equals(invitation.getId())).forEach(item -> {
            item.setStatus(DriverOperatorAssociationStatus.REJECTED);
            item.setRespondedAt(now);
        });

        return toResponse(associationRepository.saveAndFlush(invitation));
    }

    @Transactional
    public DriverOperatorAssociationResponse reject(String email, Long associationId) {
        DriverProfile driver = approvedDriver(email);
        DriverOperatorAssociation invitation = ownedPendingInvitation(driver, associationId);
        invitation.setStatus(DriverOperatorAssociationStatus.REJECTED);
        invitation.setRespondedAt(LocalDateTime.now());
        return toResponse(associationRepository.saveAndFlush(invitation));
    }

    @Transactional
    public DriverOperatorAssociationResponse remove(String email, Long associationId) {
        TransportOperator operator = approvedOperator(email);
        DriverOperatorAssociation association = associationRepository
                .findLockedByIdAndOperator(associationId, operator)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Active driver association not found"));
        if (association.getStatus() != DriverOperatorAssociationStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Only an active driver association can be removed");
        }

        List<Bus> assignedBuses = busRepository.findByOperatorAndAssignedDriver(
                operator, association.getDriver());
        assignedBuses.forEach(bus -> bus.setAssignedDriver(null));
        if (!assignedBuses.isEmpty()) busRepository.saveAll(assignedBuses);

        association.setStatus(DriverOperatorAssociationStatus.REMOVED);
        association.setRespondedAt(LocalDateTime.now());
        return toResponse(associationRepository.saveAndFlush(association));
    }

    private DriverOperatorAssociation ownedPendingInvitation(
            DriverProfile driver,
            Long associationId
    ) {
        DriverOperatorAssociation invitation = associationRepository
                .findByIdAndDriver(associationId, driver)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Invitation not found"));
        if (invitation.getStatus() != DriverOperatorAssociationStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Invitation is no longer pending");
        }
        return invitation;
    }

    private TransportOperator approvedOperator(String email) {
        User user = userByEmail(email);
        if (!"OPERATOR".equalsIgnoreCase(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Operator access is required");
        }
        TransportOperator operator = operatorRepository.findByUser(user)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Operator application not found"));
        if (operator.getVerificationStatus() != OperatorVerificationStatus.APPROVED) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Operator application is not approved");
        }
        return operator;
    }

    private DriverProfile approvedDriver(String email) {
        User user = userByEmail(email);
        if (!"DRIVER".equalsIgnoreCase(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Driver access is required");
        }
        DriverProfile driver = driverRepository.findByUser(user)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Driver profile not found"));
        if (driver.getVerificationStatus() != DriverVerificationStatus.APPROVED) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Driver application is not approved");
        }
        return driver;
    }

    private void requireApprovedDriver(DriverProfile driver) {
        if (!"DRIVER".equalsIgnoreCase(driver.getUser().getRole()) ||
                driver.getVerificationStatus() != DriverVerificationStatus.APPROVED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Driver is not eligible for association");
        }
    }

    private User userByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Authenticated user not found"));
    }

    private boolean matches(DriverProfile driver, String query) {
        if (query.isBlank()) return true;
        User user = driver.getUser();
        return contains(user.getFullName(), query) || contains(user.getEmail(), query) ||
                contains(user.getPhone(), query) || contains(driver.getLicenseNumber(), query);
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }

    private EligibleDriverResponse toEligibleResponse(DriverProfile driver) {
        User user = driver.getUser();
        return new EligibleDriverResponse(
                driver.getId(), user.getFullName(), user.getEmail(), user.getPhone(),
                driver.getLicenseNumber(), driver.getLicenseCategory(),
                driver.getVerificationStatus().name()
        );
    }

    private DriverOperatorAssociationResponse toResponse(
            DriverOperatorAssociation association
    ) {
        DriverProfile driver = association.getDriver();
        User user = driver.getUser();
        TransportOperator operator = association.getOperator();
        return new DriverOperatorAssociationResponse(
                association.getId(), driver.getId(), user.getFullName(), user.getEmail(),
                user.getPhone(), driver.getLicenseNumber(),
                driver.getVerificationStatus().name(), operator.getId(), operator.getName(),
                operator.getEmail(), operator.getPhone(), association.getStatus().name(),
                association.getInvitedAt(), association.getRespondedAt(),
                busRepository.findByOperatorAndAssignedDriver(operator, driver).stream()
                        .map(bus -> new AssociatedBusResponse(
                                bus.getId(), bus.getBusNumber(), bus.getBusName()))
                        .toList()
        );
    }
}
