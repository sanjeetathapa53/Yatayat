package com.yatayat.backend.service;

import com.yatayat.backend.dto.DriverTripOperationResponse;
import com.yatayat.backend.dto.DriverScheduledTripPageResponse;
import com.yatayat.backend.dto.DriverScheduledTripResponse;
import com.yatayat.backend.dto.OperatorLiveTripResponse;
import com.yatayat.backend.entity.*;
import com.yatayat.backend.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@Service
public class TripOperationService {
    private static final List<TripStatus> DRIVER_OPERATIONAL_STATUSES =
            List.of(TripStatus.SCHEDULED, TripStatus.BOARDING, TripStatus.IN_PROGRESS);
    private static final List<TripStatus> OPERATOR_MONITORING_STATUSES =
            List.of(TripStatus.BOARDING, TripStatus.IN_PROGRESS, TripStatus.COMPLETED);

    private final UserRepository userRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final DriverOperatorAssociationRepository associationRepository;
    private final TransportOperatorRepository operatorRepository;
    private final ScheduledTripRepository tripRepository;
    private final PassengerTripBookingRepository bookingRepository;
    private final TicketRepository ticketRepository;

    public TripOperationService(UserRepository userRepository,
                                DriverProfileRepository driverProfileRepository,
                                DriverOperatorAssociationRepository associationRepository,
                                TransportOperatorRepository operatorRepository,
                                ScheduledTripRepository tripRepository,
                                PassengerTripBookingRepository bookingRepository,
                                TicketRepository ticketRepository) {
        this.userRepository = userRepository;
        this.driverProfileRepository = driverProfileRepository;
        this.associationRepository = associationRepository;
        this.operatorRepository = operatorRepository;
        this.tripRepository = tripRepository;
        this.bookingRepository = bookingRepository;
        this.ticketRepository = ticketRepository;
    }

    public Optional<DriverTripOperationResponse> currentDriverTrip(String driverEmail) {
        DriverProfile driver = requireApprovedDriver(driverEmail);
        requireAnyActiveAssociation(driver);
        return tripRepository.findDriverOperationalTrips(
                        driver, DRIVER_OPERATIONAL_STATUSES, LocalDateTime.now())
                .stream()
                .findFirst()
                .map(this::driverResponse);
    }

    public DriverScheduledTripPageResponse assignedTrips(
            String driverEmail, String scope, int page, int size
    ) {
        DriverProfile driver = requireApprovedDriver(driverEmail);
        requireAnyActiveAssociation(driver);
        if (page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page must not be negative.");
        }
        if (size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Page size must be between 1 and 100.");
        }
        String requestedScope = scope == null || scope.isBlank()
                ? "UPCOMING" : scope.trim().toUpperCase(java.util.Locale.ROOT);
        Page<ScheduledTrip> trips;
        if ("UPCOMING".equals(requestedScope)) {
            trips = tripRepository.findDriverUpcomingTrips(
                    driver, LocalDateTime.now(), PageRequest.of(page, size));
        } else if ("HISTORY".equals(requestedScope)) {
            trips = tripRepository.findDriverTripHistory(driver, PageRequest.of(page, size));
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Scope must be UPCOMING or HISTORY.");
        }
        return new DriverScheduledTripPageResponse(
                trips.getContent().stream().map(this::scheduledTripResponse).toList(),
                trips.getNumber(), trips.getSize(), trips.getTotalElements(),
                trips.getTotalPages(), trips.isFirst(), trips.isLast());
    }

    @Transactional
    public DriverTripOperationResponse beginBoarding(String driverEmail, Long scheduledTripId) {
        DriverProfile driver = requireApprovedDriver(driverEmail);
        requireAnyActiveAssociation(driver);
        ScheduledTrip trip = lockedTrip(scheduledTripId);
        requireAssignedDriver(driver, trip);

        if (trip.getStatus() == TripStatus.CANCELLED) {
            conflict("Cancelled trips cannot begin boarding.");
        }
        if (trip.getStatus() == TripStatus.COMPLETED) {
            conflict("Completed trips cannot begin boarding.");
        }
        if (trip.getStatus() == TripStatus.BOARDING) {
            conflict("Boarding has already started for this trip.");
        }
        if (trip.getStatus() == TripStatus.IN_PROGRESS) {
            conflict("This trip is already in progress.");
        }
        if (trip.getStatus() != TripStatus.SCHEDULED) {
            conflict("Only scheduled trips can begin boarding.");
        }

        trip.setStatus(TripStatus.BOARDING);
        publishTripUpdate(trip, "BOARDING");
        return driverResponse(tripRepository.saveAndFlush(trip));
    }

    @Transactional
    public DriverTripOperationResponse start(String driverEmail, Long scheduledTripId) {
        DriverProfile driver = requireApprovedDriver(driverEmail);
        requireAnyActiveAssociation(driver);
        ScheduledTrip trip = lockedTrip(scheduledTripId);
        requireAssignedDriver(driver, trip);

        if (trip.getStatus() == TripStatus.CANCELLED) {
            conflict("Cancelled trips cannot be started.");
        }
        if (trip.getStatus() == TripStatus.COMPLETED) {
            conflict("Completed trips cannot be started.");
        }
        if (trip.getStatus() == TripStatus.IN_PROGRESS) {
            conflict("This trip has already been started.");
        }
        if (trip.getStatus() != TripStatus.BOARDING) {
            conflict("Boarding must begin before this trip can be started.");
        }

        LocalDateTime now = LocalDateTime.now();
        if (trip.getDepartureAt() != null && now.isBefore(trip.getDepartureAt().minusHours(12))) {
            conflict("This trip cannot be started this early.");
        }
        if (trip.getEstimatedArrivalAt() != null && now.isAfter(trip.getEstimatedArrivalAt().plusHours(12))) {
            conflict("This trip is too old to start.");
        }

        trip.setActualDepartureAt(now);
        trip.setStatus(TripStatus.IN_PROGRESS);
        publishTripUpdate(trip, "STARTED");
        return driverResponse(tripRepository.saveAndFlush(trip));
    }

    @Transactional
    public DriverTripOperationResponse finish(String driverEmail, Long scheduledTripId) {
        DriverProfile driver = requireApprovedDriver(driverEmail);
        requireAnyActiveAssociation(driver);
        ScheduledTrip trip = lockedTrip(scheduledTripId);
        requireAssignedDriver(driver, trip);

        if (trip.getStatus() == TripStatus.CANCELLED) {
            conflict("Cancelled trips cannot be finished.");
        }
        if (trip.getStatus() == TripStatus.COMPLETED) {
            conflict("This trip is already completed.");
        }
        if (trip.getActualDepartureAt() == null || trip.getStatus() == TripStatus.SCHEDULED) {
            conflict("Start this trip before finishing it.");
        }
        if (trip.getStatus() != TripStatus.BOARDING && trip.getStatus() != TripStatus.IN_PROGRESS) {
            conflict("This trip is not active.");
        }

        trip.setActualArrivalAt(LocalDateTime.now());
        trip.setStatus(TripStatus.COMPLETED);
        publishTripUpdate(trip, "COMPLETED");
        return driverResponse(tripRepository.saveAndFlush(trip));
    }

    public List<OperatorLiveTripResponse> operatorLiveTrips(String operatorEmail) {
        TransportOperator operator = requireApprovedOperator(operatorEmail);
        return tripRepository.findOperatorLiveTrips(operator, OPERATOR_MONITORING_STATUSES)
                .stream()
                .map(this::operatorResponse)
                .toList();
    }

    public void publishTripUpdate(ScheduledTrip trip, String eventType) {
        // Realtime foundation: WebSocket/SSE publishers can hook into this method later.
        // Phase 7 persists lifecycle state only; it does not broadcast yet.
    }

    private DriverProfile requireApprovedDriver(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Authenticated driver not found."));
        if (!"DRIVER".equalsIgnoreCase(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Driver access is required.");
        }
        DriverProfile driver = driverProfileRepository.findByUser(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Approved driver profile is required."));
        if (!driver.isApproved() || driver.isLicenseExpired()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Approved active driver profile is required.");
        }
        return driver;
    }

    private TransportOperator requireApprovedOperator(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Authenticated operator not found."));
        if (!"OPERATOR".equalsIgnoreCase(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Operator access is required.");
        }
        TransportOperator operator = operatorRepository.findByUser(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Operator application not found."));
        if (!operator.isApproved()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Operator application is not approved.");
        }
        return operator;
    }

    private void requireAnyActiveAssociation(DriverProfile driver) {
        associationRepository.findByDriverAndStatus(driver, DriverOperatorAssociationStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Active operator association is required."));
    }

    private ScheduledTrip lockedTrip(Long scheduledTripId) {
        if (scheduledTripId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found.");
        }
        return tripRepository.findByIdForOperation(scheduledTripId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Trip not found."));
    }

    private void requireAssignedDriver(DriverProfile driver, ScheduledTrip trip) {
        if (trip.getDriver() == null || !trip.getDriver().getId().equals(driver.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Trip not found.");
        }
        associationRepository.findByDriverAndOperator(driver, trip.getOperator())
                .filter(association -> association.getStatus() == DriverOperatorAssociationStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Active operator association is required for this trip."));
    }

    private DriverTripOperationResponse driverResponse(ScheduledTrip trip) {
        long confirmed = confirmedSeats(trip);
        long boarded = boardedTickets(trip);
        return new DriverTripOperationResponse(
                trip.getId(),
                trip.getRoute().getCode(),
                trip.getRoute().getName(),
                trip.getRoute().getOrigin(),
                trip.getRoute().getDestination(),
                trip.getBus().getBusName(),
                trip.getBus().getBusNumber(),
                trip.getOperator().getName(),
                trip.getDepartureAt(),
                trip.getEstimatedArrivalAt(),
                trip.getActualDepartureAt(),
                trip.getActualArrivalAt(),
                trip.getStatus().name(),
                confirmed,
                boarded,
                Math.max(0, confirmed - boarded)
        );
    }

    private DriverScheduledTripResponse scheduledTripResponse(ScheduledTrip trip) {
        return new DriverScheduledTripResponse(
                trip.getId(), trip.getStatus().name(),
                trip.getRoute().getId(), trip.getRoute().getName(),
                trip.getRoute().getOrigin(), trip.getRoute().getDestination(),
                trip.getBus().getId(), trip.getBus().getBusNumber(), trip.getBus().getBusName(),
                trip.getOperator().getId(), trip.getOperator().getName(),
                trip.getDepartureAt(), trip.getEstimatedArrivalAt(), trip.getFare(),
                trip.getBoardingNotes(),
                trip.getStatus() == TripStatus.SCHEDULED,
                trip.getStatus() == TripStatus.BOARDING,
                trip.getStatus() == TripStatus.IN_PROGRESS);
    }

    private OperatorLiveTripResponse operatorResponse(ScheduledTrip trip) {
        return new OperatorLiveTripResponse(
                trip.getId(),
                trip.getRoute().getCode(),
                trip.getRoute().getName(),
                trip.getRoute().getOrigin(),
                trip.getRoute().getDestination(),
                trip.getBus().getBusName(),
                trip.getBus().getBusNumber(),
                trip.getDriver().getUser().getFullName(),
                trip.getDepartureAt(),
                trip.getEstimatedArrivalAt(),
                trip.getActualDepartureAt(),
                trip.getActualArrivalAt(),
                trip.getStatus().name(),
                confirmedSeats(trip),
                boardedTickets(trip)
        );
    }

    private long confirmedSeats(ScheduledTrip trip) {
        Long count = bookingRepository.sumConfirmedSeatsByTrip(trip);
        return count == null ? 0 : count;
    }

    private long boardedTickets(ScheduledTrip trip) {
        return ticketRepository.countByBookingScheduledTripAndStatus(trip, TicketStatus.USED);
    }

    private void conflict(String message) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, message);
    }
}
