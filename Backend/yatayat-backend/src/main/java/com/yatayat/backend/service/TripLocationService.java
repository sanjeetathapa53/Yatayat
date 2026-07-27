package com.yatayat.backend.service;

import com.yatayat.backend.dto.TripLocationResponse;
import com.yatayat.backend.dto.TripLocationUpdateRequest;
import com.yatayat.backend.entity.*;
import com.yatayat.backend.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TripLocationService {

    private final UserRepository userRepository;
    private final DriverProfileRepository driverRepository;
    private final DriverOperatorAssociationRepository associationRepository;
    private final ScheduledTripRepository tripRepository;
    private final TripLocationRepository locationRepository;

    public TripLocationService(
            UserRepository userRepository,
            DriverProfileRepository driverRepository,
            DriverOperatorAssociationRepository associationRepository,
            ScheduledTripRepository tripRepository,
            TripLocationRepository locationRepository
    ) {
        this.userRepository = userRepository;
        this.driverRepository = driverRepository;
        this.associationRepository = associationRepository;
        this.tripRepository = tripRepository;
        this.locationRepository = locationRepository;
    }

    @Transactional
    public TripLocationResponse update(
            String authenticatedEmail,
            Long tripId,
            TripLocationUpdateRequest request
    ) {
        DriverProfile driver = requireApprovedDriver(authenticatedEmail);
        ScheduledTrip trip = findTrip(tripId);

        if (trip.getDriver() == null || !trip.getDriver().getId().equals(driver.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found.");
        }
        associationRepository.findByDriverAndOperator(driver, trip.getOperator())
                .filter(association ->
                        association.getStatus() == DriverOperatorAssociationStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Active operator association is required for this trip."
                ));
        if (trip.getStatus() != TripStatus.IN_PROGRESS) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Location can only be updated for a trip that is in progress."
            );
        }

        TripLocation location = locationRepository.findByTrip(trip)
                .orElseGet(() -> {
                    TripLocation created = new TripLocation();
                    created.setTrip(trip);
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

    private ScheduledTrip findTrip(Long tripId) {
        if (tripId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found.");
        }
        return tripRepository.findByIdForOperation(tripId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Trip not found."));
    }

    private TripLocationResponse toResponse(TripLocation location) {
        return new TripLocationResponse(
                location.getTrip().getId(), location.getLatitude(), location.getLongitude(),
                location.getAccuracy(), location.getSpeed(), location.getHeading(),
                location.getUpdatedAt()
        );
    }
}
