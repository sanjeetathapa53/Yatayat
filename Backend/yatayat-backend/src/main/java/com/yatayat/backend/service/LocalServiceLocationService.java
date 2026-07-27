package com.yatayat.backend.service;

import com.yatayat.backend.dto.LocalServiceLocationResponse;
import com.yatayat.backend.dto.TripLocationUpdateRequest;
import com.yatayat.backend.entity.*;
import com.yatayat.backend.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LocalServiceLocationService {
    private final UserRepository userRepository;
    private final DriverProfileRepository driverRepository;
    private final DriverOperatorAssociationRepository associationRepository;
    private final LocalServiceRunRepository runRepository;
    private final LocalServiceLocationRepository locationRepository;

    public LocalServiceLocationService(
            UserRepository userRepository,
            DriverProfileRepository driverRepository,
            DriverOperatorAssociationRepository associationRepository,
            LocalServiceRunRepository runRepository,
            LocalServiceLocationRepository locationRepository
    ) {
        this.userRepository = userRepository;
        this.driverRepository = driverRepository;
        this.associationRepository = associationRepository;
        this.runRepository = runRepository;
        this.locationRepository = locationRepository;
    }

    @Transactional
    public LocalServiceLocationResponse update(
            String authenticatedEmail,
            Long runId,
            TripLocationUpdateRequest request
    ) {
        DriverProfile driver = requireApprovedDriver(authenticatedEmail);
        LocalServiceRun run = lockedDriverRun(driver, runId);

        associationRepository.findByDriverAndOperator(driver, run.getOperator())
                .filter(association ->
                        association.getStatus() == DriverOperatorAssociationStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Active operator association is required for this local service."
                ));
        if (run.getStatus() != LocalServiceRunStatus.IN_SERVICE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Location can only be updated for a local service that is in service."
            );
        }

        LocalServiceLocation location = locationRepository.findByRun(run)
                .orElseGet(() -> {
                    LocalServiceLocation created = new LocalServiceLocation();
                    created.setRun(run);
                    return created;
                });
        location.setLatitude(request.latitude());
        location.setLongitude(request.longitude());
        location.setAccuracy(request.accuracy());
        location.setSpeed(request.speed());
        location.setHeading(request.heading());
        return toResponse(locationRepository.saveAndFlush(location));
    }

    private DriverProfile requireApprovedDriver(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Authenticated driver not found."));
        if (!"DRIVER".equalsIgnoreCase(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Driver access is required.");
        }
        DriverProfile driver = driverRepository.findByUser(user)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Approved driver profile is required."));
        if (!driver.isApproved() || driver.isLicenseExpired()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Approved active driver profile is required.");
        }
        return driver;
    }

    private LocalServiceRun lockedDriverRun(DriverProfile driver, Long runId) {
        if (runId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Local service not found.");
        }
        return runRepository.findByIdAndDriverForOperation(runId, driver)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Local service not found."));
    }

    private LocalServiceLocationResponse toResponse(LocalServiceLocation location) {
        return new LocalServiceLocationResponse(
                location.getRun().getId(),
                location.getLatitude(),
                location.getLongitude(),
                location.getAccuracy(),
                location.getSpeed(),
                location.getHeading(),
                location.getUpdatedAt()
        );
    }
}
