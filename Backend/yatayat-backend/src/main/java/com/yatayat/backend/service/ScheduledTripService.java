package com.yatayat.backend.service;

import com.yatayat.backend.dto.*;
import com.yatayat.backend.entity.*;
import com.yatayat.backend.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ScheduledTripService {

    private final UserRepository userRepository;
    private final TransportOperatorRepository operatorRepository;
    private final RouteRepository routeRepository;
    private final BusRepository busRepository;
    private final DriverProfileRepository driverRepository;
    private final DriverOperatorAssociationRepository associationRepository;
    private final ScheduledTripRepository tripRepository;
    private final PassengerTripBookingRepository bookingRepository;
    private final TicketRepository ticketRepository;

    public ScheduledTripService(
            UserRepository userRepository,
            TransportOperatorRepository operatorRepository,
            RouteRepository routeRepository,
            BusRepository busRepository,
            DriverProfileRepository driverRepository,
            DriverOperatorAssociationRepository associationRepository,
            ScheduledTripRepository tripRepository,
            PassengerTripBookingRepository bookingRepository,
            TicketRepository ticketRepository
    ) {
        this.userRepository = userRepository;
        this.operatorRepository = operatorRepository;
        this.routeRepository = routeRepository;
        this.busRepository = busRepository;
        this.driverRepository = driverRepository;
        this.associationRepository = associationRepository;
        this.tripRepository = tripRepository;
        this.bookingRepository = bookingRepository;
        this.ticketRepository = ticketRepository;
    }

    public TripEligibilityResponse getEligibility(String email) {
        TransportOperator operator = approvedOperator(email);
        LocalDate today = LocalDate.now();

        List<RouteEligibilityResponse> routes = routeRepository
                .findByStatusOrderByCodeAsc(RouteStatus.ACTIVE).stream()
                .filter(route -> route.getTripType() == TripType.OUT_OF_VALLEY)
                .map(route -> new RouteEligibilityResponse(
                        route.getId(), route.getCode(), route.getName(),
                        route.getOrigin(), route.getDestination(), route.getTripType().name()
                )).toList();

        List<BusEligibilityResponse> buses = eligibleBuses(operator, today);
        List<DriverEligibilityResponse> drivers = eligibleDrivers(operator, today);

        return new TripEligibilityResponse(routes, buses, drivers);
    }

    public List<BusEligibilityResponse> getEligibleBuses(String email) {
        return eligibleBuses(approvedOperator(email), LocalDate.now());
    }

    public List<DriverEligibilityResponse> getEligibleDrivers(String email) {
        return eligibleDrivers(approvedOperator(email), LocalDate.now());
    }

    @Transactional
    public TripResponse create(String email, TripCreateRequest request) {
        TransportOperator operator = approvedOperator(email);
        validateRequest(request == null ? null : request.routeId(),
                request == null ? null : request.busId(),
                request == null ? null : request.driverId(),
                request == null ? null : request.departureAt(),
                request == null ? null : request.estimatedArrivalAt(),
                request == null ? null : request.fare(),
                request == null ? null : request.boardingNotes());

        EligibleResources resources = eligibleResources(
                operator, request.routeId(), request.busId(), request.driverId(),
                request.departureAt()
        );
        ensureNoOverlap(resources.bus(), resources.driver(),
                request.departureAt(), request.estimatedArrivalAt(), null);

        ScheduledTrip trip = new ScheduledTrip();
        trip.setOperator(operator);
        apply(trip, resources, request.departureAt(), request.estimatedArrivalAt(),
                request.fare(), request.boardingNotes());
        trip.setStatus(TripStatus.SCHEDULED);
        return toResponse(tripRepository.saveAndFlush(trip));
    }

    public List<TripSummaryResponse> list(
            String email, TripStatus status, LocalDateTime from, LocalDateTime to
    ) {
        TransportOperator operator = approvedOperator(email);
        if (from != null && to != null && to.isBefore(from)) {
            badRequest("Filter end must not be before filter start");
        }
        return tripRepository.findByOperatorOrderByDepartureAtDesc(operator).stream()
                .filter(trip -> status == null || trip.getStatus() == status)
                .filter(trip -> from == null || !trip.getDepartureAt().isBefore(from))
                .filter(trip -> to == null || !trip.getDepartureAt().isAfter(to))
                .map(this::toSummary)
                .toList();
    }

    public TripResponse get(String email, Long tripId) {
        TransportOperator operator = approvedOperator(email);
        return toResponse(ownedTrip(operator, tripId));
    }

    @Transactional
    public TripResponse update(String email, Long tripId, TripUpdateRequest request) {
        TransportOperator operator = approvedOperator(email);
        ScheduledTrip trip = ownedTrip(operator, tripId);
        if (trip.getStatus() != TripStatus.SCHEDULED) {
            conflict("Only scheduled trips can be edited");
        }
        ensureNoConfirmedBookingsForEdit(trip);
        validateRequest(request == null ? null : request.routeId(),
                request == null ? null : request.busId(),
                request == null ? null : request.driverId(),
                request == null ? null : request.departureAt(),
                request == null ? null : request.estimatedArrivalAt(),
                request == null ? null : request.fare(),
                request == null ? null : request.boardingNotes());

        EligibleResources resources = eligibleResources(
                operator, request.routeId(), request.busId(), request.driverId(),
                request.departureAt()
        );
        ensureNoOverlap(resources.bus(), resources.driver(), request.departureAt(),
                request.estimatedArrivalAt(), trip.getId());
        apply(trip, resources, request.departureAt(), request.estimatedArrivalAt(),
                request.fare(), request.boardingNotes());
        return toResponse(tripRepository.saveAndFlush(trip));
    }

    @Transactional
    public TripResponse assign(String email, Long tripId, TripAssignmentRequest request) {
        TransportOperator operator = approvedOperator(email);
        ScheduledTrip trip = ownedTrip(operator, tripId);
        if (trip.getStatus() == TripStatus.IN_PROGRESS ||
                trip.getStatus() == TripStatus.COMPLETED ||
                trip.getStatus() == TripStatus.CANCELLED) {
            conflict("Assignment can be changed only before the trip is active or completed");
        }
        if (!trip.getDepartureAt().isAfter(LocalDateTime.now())) {
            conflict("Assignment cannot be changed after departure time");
        }
        if (request == null || request.busId() == null || request.driverId() == null) {
            badRequest("Bus and driver are required");
        }

        EligibleResources resources = eligibleResources(
                operator, trip.getRoute().getId(), request.busId(), request.driverId(),
                trip.getDepartureAt()
        );
        long confirmedSeats = confirmedSeats(trip);
        if (resources.bus().getSeatCapacity() < confirmedSeats) {
            conflict("Selected bus capacity is below the confirmed passenger count");
        }
        ensureNoOverlap(resources.bus(), resources.driver(), trip.getDepartureAt(),
                trip.getEstimatedArrivalAt(), trip.getId());
        trip.setBus(resources.bus());
        trip.setDriver(resources.driver());
        trip.setSeatCapacitySnapshot(resources.bus().getSeatCapacity());
        return toResponse(tripRepository.saveAndFlush(trip));
    }

    @Transactional
    public TripResponse cancel(String email, Long tripId, TripCancellationRequest request) {
        TransportOperator operator = approvedOperator(email);
        ScheduledTrip trip = ownedTrip(operator, tripId);
        if (trip.getStatus() != TripStatus.SCHEDULED &&
                trip.getStatus() != TripStatus.BOARDING) {
            conflict("Only scheduled or boarding trips can be cancelled");
        }
        if (confirmedSeats(trip) > 0) {
            conflict("Trips with confirmed bookings cannot be cancelled from this screen");
        }
        String reason = request == null ? null : request.reason();
        if (reason != null && reason.trim().length() > 1000) {
            badRequest("Cancellation reason must not exceed 1000 characters");
        }
        trip.setStatus(TripStatus.CANCELLED);
        trip.setCancellationReason(reason);
        return toResponse(tripRepository.saveAndFlush(trip));
    }

    private List<BusEligibilityResponse> eligibleBuses(TransportOperator operator, LocalDate tripDate) {
        return busRepository
                .findByOperatorOrderByCreatedAtDesc(operator).stream()
                .filter(bus -> bus.getStatus() == BusStatus.APPROVED)
                .filter(bus -> bus.getPermitExpiryDate() != null &&
                        !bus.getPermitExpiryDate().isBefore(tripDate))
                .filter(bus -> bus.getInsuranceExpiryDate() != null &&
                        !bus.getInsuranceExpiryDate().isBefore(tripDate))
                .map(bus -> new BusEligibilityResponse(
                        bus.getId(), bus.getBusNumber(), bus.getBusName(),
                        bus.getBusType(), bus.getSeatCapacity()
                )).toList();
    }

    private List<DriverEligibilityResponse> eligibleDrivers(TransportOperator operator, LocalDate tripDate) {
        return associationRepository
                .findByOperatorAndStatusOrderByInvitedAtDesc(
                        operator, DriverOperatorAssociationStatus.ACTIVE
                ).stream()
                .map(DriverOperatorAssociation::getDriver)
                .filter(DriverProfile::isApproved)
                .filter(driver -> driver.getLicenseExpiryDate() != null &&
                        !driver.getLicenseExpiryDate().isBefore(tripDate))
                .map(driver -> new DriverEligibilityResponse(
                        driver.getId(), driver.getUser().getFullName(),
                        driver.getLicenseNumber(), driver.getLicenseCategory()
                )).toList();
    }

    private EligibleResources eligibleResources(
            TransportOperator operator, Long routeId, Long busId,
            Long driverId, LocalDateTime departureAt
    ) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> notFound("Route not found"));
        if (route.getStatus() != RouteStatus.ACTIVE) {
            conflict("Selected route is not active");
        }
        if (route.getTripType() != TripType.OUT_OF_VALLEY) {
            conflict("Only out-of-valley routes support scheduled seat booking");
        }

        Bus bus = busRepository.findLockedByIdAndOperator(busId, operator)
                .orElseThrow(() -> notFound("Bus not found"));
        if (bus.getStatus() != BusStatus.APPROVED) {
            conflict("Selected bus is not approved");
        }
        LocalDate tripDate = departureAt.toLocalDate();
        if (bus.getPermitExpiryDate() == null ||
                bus.getPermitExpiryDate().isBefore(tripDate)) {
            conflict("Selected bus permit is not valid for the trip date");
        }
        if (bus.getInsuranceExpiryDate() == null ||
                bus.getInsuranceExpiryDate().isBefore(tripDate)) {
            conflict("Selected bus insurance is not valid for the trip date");
        }

        DriverProfile driver = driverRepository.findLockedById(driverId)
                .orElseThrow(() -> notFound("Driver not found"));
        DriverOperatorAssociation association = associationRepository
                .findByDriverAndOperator(driver, operator)
                .orElseThrow(() -> notFound("Driver not found"));
        if (association.getStatus() != DriverOperatorAssociationStatus.ACTIVE) {
            conflict("Selected driver does not have an active operator association");
        }
        if (!driver.isApproved()) {
            conflict("Selected driver is not approved");
        }
        if (driver.getLicenseExpiryDate() == null ||
                driver.getLicenseExpiryDate().isBefore(tripDate)) {
            conflict("Selected driver licence is not valid for the trip date");
        }
        return new EligibleResources(route, bus, driver);
    }

    private void ensureNoOverlap(
            Bus bus, DriverProfile driver, LocalDateTime departure,
            LocalDateTime arrival, Long excludedId
    ) {
        List<ScheduledTrip> busConflicts = tripRepository.findBusConflictsForUpdate(
                bus, departure, arrival, excludedId);
        if (!busConflicts.isEmpty()) {
            conflict("BUS_SCHEDULE_CONFLICT: " + conflictSummary(busConflicts.get(0)));
        }
        List<ScheduledTrip> driverConflicts = tripRepository.findDriverConflictsForUpdate(
                driver, departure, arrival, excludedId);
        if (!driverConflicts.isEmpty()) {
            conflict("DRIVER_SCHEDULE_CONFLICT: " + conflictSummary(driverConflicts.get(0)));
        }
    }

    private void validateRequest(
            Long routeId, Long busId, Long driverId, LocalDateTime departure,
            LocalDateTime arrival, java.math.BigDecimal fare, String notes
    ) {
        if (routeId == null || busId == null || driverId == null) {
            badRequest("Route, bus and driver are required");
        }
        if (departure == null || arrival == null) {
            badRequest("Departure and estimated arrival are required");
        }
        if (!departure.isAfter(LocalDateTime.now())) {
            badRequest("Departure must be in the future");
        }
        if (!arrival.isAfter(departure)) {
            badRequest("Estimated arrival must be after departure");
        }
        if (fare == null || fare.signum() <= 0) {
            badRequest("Fare must be greater than zero");
        }
        if (notes != null && notes.trim().length() > 1000) {
            badRequest("Boarding notes must not exceed 1000 characters");
        }
    }

    private void apply(
            ScheduledTrip trip, EligibleResources resources,
            LocalDateTime departure, LocalDateTime arrival,
            java.math.BigDecimal fare, String notes
    ) {
        trip.setRoute(resources.route());
        trip.setBus(resources.bus());
        trip.setDriver(resources.driver());
        trip.setDepartureAt(departure);
        trip.setEstimatedArrivalAt(arrival);
        trip.setFare(fare);
        trip.setSeatCapacitySnapshot(resources.bus().getSeatCapacity());
        trip.setBoardingNotes(notes);
    }

    private TransportOperator approvedOperator(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Authenticated user not found"));
        if (!"OPERATOR".equalsIgnoreCase(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Operator access is required");
        }
        TransportOperator operator = operatorRepository.findByUser(user)
                .orElseThrow(() -> notFound("Operator application not found"));
        if (operator.getVerificationStatus() != OperatorVerificationStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Operator application is not approved");
        }
        return operator;
    }

    private ScheduledTrip ownedTrip(TransportOperator operator, Long id) {
        return tripRepository.findByIdAndOperator(id, operator)
                .orElseThrow(() -> notFound("Trip not found"));
    }

    private TripSummaryResponse toSummary(ScheduledTrip trip) {
        return new TripSummaryResponse(
                trip.getId(), trip.getRoute().getCode(), trip.getRoute().getName(),
                trip.getRoute().getOrigin(), trip.getRoute().getDestination(),
                trip.getBus().getBusNumber(), trip.getDriver().getUser().getFullName(),
                trip.getDepartureAt(), trip.getEstimatedArrivalAt(), trip.getFare(),
                trip.getSeatCapacitySnapshot(), trip.getStatus().name(),
                confirmedSeats(trip), boardedTickets(trip), assignmentComplete(trip)
        );
    }

    private TripResponse toResponse(ScheduledTrip trip) {
        return new TripResponse(
                trip.getId(), trip.getRoute().getId(), trip.getRoute().getCode(),
                trip.getRoute().getName(), trip.getRoute().getOrigin(),
                trip.getRoute().getDestination(), trip.getBus().getId(),
                trip.getBus().getBusNumber(), trip.getBus().getBusName(),
                trip.getDriver().getId(), trip.getDriver().getUser().getFullName(),
                trip.getDepartureAt(), trip.getEstimatedArrivalAt(),
                trip.getActualDepartureAt(), trip.getActualArrivalAt(), trip.getFare(),
                trip.getSeatCapacitySnapshot(), trip.getStatus().name(),
                trip.getBoardingNotes(), trip.getCancellationReason(),
                trip.getCreatedAt(), trip.getUpdatedAt(),
                confirmedSeats(trip), boardedTickets(trip), assignmentComplete(trip)
        );
    }

    private void ensureNoConfirmedBookingsForEdit(ScheduledTrip trip) {
        if (bookingRepository.existsByScheduledTripAndStatus(trip, BookingStatus.CONFIRMED)) {
            conflict("Trips with confirmed bookings cannot be edited");
        }
    }

    private long confirmedSeats(ScheduledTrip trip) {
        Long count = bookingRepository.sumConfirmedSeatsByTrip(trip);
        return count == null ? 0 : count;
    }

    private long boardedTickets(ScheduledTrip trip) {
        return ticketRepository.countByBookingScheduledTripAndStatus(trip, TicketStatus.USED);
    }

    private boolean assignmentComplete(ScheduledTrip trip) {
        return trip.getBus() != null && trip.getDriver() != null;
    }

    private String conflictSummary(ScheduledTrip trip) {
        return "%s from %s to %s conflicts with %s - %s"
                .formatted(trip.getRoute().getName(), trip.getDepartureAt(),
                        trip.getEstimatedArrivalAt(), trip.getRoute().getOrigin(),
                        trip.getRoute().getDestination());
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }
    private void badRequest(String message) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
    private void conflict(String message) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, message);
    }

    private record EligibleResources(Route route, Bus bus, DriverProfile driver) {}
}
