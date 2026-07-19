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
import java.time.LocalTime;
import java.util.List;

@Service
public class LocalServiceRunService {
    private static final List<LocalServiceRunStatus> BLOCKING_LOCAL_STATUSES =
            List.of(LocalServiceRunStatus.PLANNED, LocalServiceRunStatus.READY, LocalServiceRunStatus.IN_SERVICE);

    private final UserRepository userRepository;
    private final TransportOperatorRepository operatorRepository;
    private final RouteRepository routeRepository;
    private final RouteStopRepository routeStopRepository;
    private final BusRepository busRepository;
    private final DriverProfileRepository driverRepository;
    private final DriverOperatorAssociationRepository associationRepository;
    private final ScheduledTripRepository scheduledTripRepository;
    private final LocalServiceRunRepository localRunRepository;

    public LocalServiceRunService(
            UserRepository userRepository,
            TransportOperatorRepository operatorRepository,
            RouteRepository routeRepository,
            RouteStopRepository routeStopRepository,
            BusRepository busRepository,
            DriverProfileRepository driverRepository,
            DriverOperatorAssociationRepository associationRepository,
            ScheduledTripRepository scheduledTripRepository,
            LocalServiceRunRepository localRunRepository
    ) {
        this.userRepository = userRepository;
        this.operatorRepository = operatorRepository;
        this.routeRepository = routeRepository;
        this.routeStopRepository = routeStopRepository;
        this.busRepository = busRepository;
        this.driverRepository = driverRepository;
        this.associationRepository = associationRepository;
        this.scheduledTripRepository = scheduledTripRepository;
        this.localRunRepository = localRunRepository;
    }

    public LocalServiceOptionsResponse options(String email) {
        TransportOperator operator = approvedOperator(email);
        LocalDate today = LocalDate.now();
        return new LocalServiceOptionsResponse(
                routeRepository.findByStatusAndTripTypeOrderByCodeAsc(RouteStatus.ACTIVE, TripType.LOCAL)
                        .stream()
                        .map(route -> new RouteEligibilityResponse(
                                route.getId(), route.getCode(), route.getName(),
                                route.getOrigin(), route.getDestination(), route.getTripType().name()
                        )).toList(),
                eligibleBuses(operator, today),
                eligibleDrivers(operator, today)
        );
    }

    public List<BusEligibilityResponse> eligibleBuses(String email) {
        return eligibleBuses(approvedOperator(email), LocalDate.now());
    }

    public List<DriverEligibilityResponse> eligibleDrivers(String email) {
        return eligibleDrivers(approvedOperator(email), LocalDate.now());
    }

    public List<LocalServiceRunResponse> listOperatorRuns(
            String email, LocalServiceRunStatus status, LocalDate serviceDate
    ) {
        TransportOperator operator = approvedOperator(email);
        return localRunRepository.findByOperatorOrderByServiceDateDescPlannedStartTimeDesc(operator)
                .stream()
                .filter(run -> status == null || run.getStatus() == status)
                .filter(run -> serviceDate == null || run.getServiceDate().equals(serviceDate))
                .map(this::toResponse)
                .toList();
    }

    public LocalServiceRunResponse getOperatorRun(String email, Long id) {
        TransportOperator operator = approvedOperator(email);
        return toResponse(ownedRun(operator, id));
    }

    @Transactional
    public LocalServiceRunResponse create(String email, LocalServiceRunRequest request) {
        TransportOperator operator = approvedOperator(email);
        validateRequest(request);
        EligibleLocalResources resources = eligibleResources(
                operator, request.routeId(), request.busId(), request.driverId(),
                request.serviceDate()
        );
        ensureNoOverlap(resources.bus(), resources.driver(), request.serviceDate(),
                request.plannedStartTime(), request.plannedEndTime(), null);

        LocalServiceRun run = new LocalServiceRun();
        run.setOperator(operator);
        apply(run, resources, request);
        run.setStatus(LocalServiceRunStatus.PLANNED);
        return toResponse(localRunRepository.saveAndFlush(run));
    }

    @Transactional
    public LocalServiceRunResponse update(String email, Long id, LocalServiceRunRequest request) {
        TransportOperator operator = approvedOperator(email);
        LocalServiceRun run = ownedRun(operator, id);
        if (run.getStatus() != LocalServiceRunStatus.PLANNED && run.getStatus() != LocalServiceRunStatus.READY) {
            conflict("Only planned or ready local services can be edited.");
        }
        validateRequest(request);
        EligibleLocalResources resources = eligibleResources(
                operator, request.routeId(), request.busId(), request.driverId(),
                request.serviceDate()
        );
        ensureNoOverlap(resources.bus(), resources.driver(), request.serviceDate(),
                request.plannedStartTime(), request.plannedEndTime(), run.getId());
        apply(run, resources, request);
        return toResponse(localRunRepository.saveAndFlush(run));
    }

    @Transactional
    public LocalServiceRunResponse cancel(String email, Long id, LocalServiceCancellationRequest request) {
        TransportOperator operator = approvedOperator(email);
        LocalServiceRun run = ownedRun(operator, id);
        if (run.getStatus() == LocalServiceRunStatus.IN_SERVICE ||
                run.getStatus() == LocalServiceRunStatus.COMPLETED ||
                run.getStatus() == LocalServiceRunStatus.CANCELLED) {
            conflict("This local service cannot be cancelled from planning.");
        }
        String reason = request == null ? null : request.reason();
        if (reason != null && reason.trim().length() > 1000) {
            badRequest("Cancellation reason must not exceed 1000 characters.");
        }
        run.setStatus(LocalServiceRunStatus.CANCELLED);
        run.setNotes(appendCancellationNote(run.getNotes(), reason));
        return toResponse(localRunRepository.saveAndFlush(run));
    }

    public List<LocalServiceRunResponse> listDriverRuns(String email) {
        DriverProfile driver = approvedDriver(email);
        return localRunRepository.findByDriverOrderByServiceDateAscPlannedStartTimeAsc(driver)
                .stream()
                .filter(run -> run.getStatus() != LocalServiceRunStatus.CANCELLED)
                .map(this::toResponse)
                .toList();
    }

    public LocalServiceRunResponse getDriverRun(String email, Long id) {
        DriverProfile driver = approvedDriver(email);
        return toResponse(localRunRepository.findByIdAndDriver(id, driver)
                .orElseThrow(() -> notFound("Local service not found.")));
    }

    private List<BusEligibilityResponse> eligibleBuses(TransportOperator operator, LocalDate serviceDate) {
        return busRepository.findByOperatorOrderByCreatedAtDesc(operator)
                .stream()
                .filter(bus -> bus.getStatus() == BusStatus.APPROVED)
                .filter(bus -> bus.getPermitExpiryDate() != null && !bus.getPermitExpiryDate().isBefore(serviceDate))
                .filter(bus -> bus.getInsuranceExpiryDate() != null && !bus.getInsuranceExpiryDate().isBefore(serviceDate))
                .map(bus -> new BusEligibilityResponse(
                        bus.getId(), bus.getBusNumber(), bus.getBusName(),
                        bus.getBusType(), bus.getSeatCapacity()
                )).toList();
    }

    private List<DriverEligibilityResponse> eligibleDrivers(TransportOperator operator, LocalDate serviceDate) {
        return associationRepository
                .findByOperatorAndStatusOrderByInvitedAtDesc(operator, DriverOperatorAssociationStatus.ACTIVE)
                .stream()
                .map(DriverOperatorAssociation::getDriver)
                .filter(DriverProfile::isApproved)
                .filter(driver -> driver.getLicenseExpiryDate() != null &&
                        !driver.getLicenseExpiryDate().isBefore(serviceDate))
                .map(driver -> new DriverEligibilityResponse(
                        driver.getId(),
                        driver.getUser().getFullName(),
                        driver.getLicenseNumber(),
                        driver.getLicenseCategory()
                )).toList();
    }

    private EligibleLocalResources eligibleResources(
            TransportOperator operator, Long routeId, Long busId, Long driverId, LocalDate serviceDate
    ) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> notFound("Route not found."));
        if (route.getTripType() != TripType.LOCAL) {
            conflict("Only local routes can be assigned to local services.");
        }
        if (route.getStatus() != RouteStatus.ACTIVE) {
            conflict("Selected local route is not active.");
        }
        if (routeStopRepository.findByRouteIdAndActiveTrueOrderByStopOrderAsc(route.getId()).size() < 2) {
            conflict("Selected local route must have at least two active stops.");
        }

        Bus bus = busRepository.findLockedByIdAndOperator(busId, operator)
                .orElseThrow(() -> notFound("Bus not found."));
        if (bus.getStatus() != BusStatus.APPROVED) {
            conflict("Selected bus is not approved.");
        }
        if (bus.getPermitExpiryDate() == null || bus.getPermitExpiryDate().isBefore(serviceDate)) {
            conflict("Selected bus permit is not valid for the service date.");
        }
        if (bus.getInsuranceExpiryDate() == null || bus.getInsuranceExpiryDate().isBefore(serviceDate)) {
            conflict("Selected bus insurance is not valid for the service date.");
        }

        DriverProfile driver = driverRepository.findLockedById(driverId)
                .orElseThrow(() -> notFound("Driver not found."));
        DriverOperatorAssociation association = associationRepository
                .findByDriverAndOperator(driver, operator)
                .orElseThrow(() -> notFound("Driver not found."));
        if (association.getStatus() != DriverOperatorAssociationStatus.ACTIVE) {
            conflict("Selected driver does not have an active operator association.");
        }
        if (!driver.isApproved()) {
            conflict("Selected driver is not approved.");
        }
        if (driver.getLicenseExpiryDate() == null || driver.getLicenseExpiryDate().isBefore(serviceDate)) {
            conflict("Selected driver licence is not valid for the service date.");
        }
        return new EligibleLocalResources(route, bus, driver);
    }

    private void ensureNoOverlap(
            Bus bus, DriverProfile driver, LocalDate serviceDate,
            LocalTime startTime, LocalTime endTime, Long excludedId
    ) {
        if (!localRunRepository.findBusConflictsForUpdate(
                bus, serviceDate, startTime, endTime, BLOCKING_LOCAL_STATUSES, excludedId
        ).isEmpty()) {
            conflict("Bus is already assigned during this time.");
        }
        if (!localRunRepository.findDriverConflictsForUpdate(
                driver, serviceDate, startTime, endTime, BLOCKING_LOCAL_STATUSES, excludedId
        ).isEmpty()) {
            conflict("Driver is already assigned during this time.");
        }
        LocalDateTime start = LocalDateTime.of(serviceDate, startTime);
        LocalDateTime end = LocalDateTime.of(serviceDate, endTime);
        if (!scheduledTripRepository.findBusConflictsForUpdate(bus, start, end, null).isEmpty()) {
            conflict("Bus is already assigned during this time.");
        }
        if (!scheduledTripRepository.findDriverConflictsForUpdate(driver, start, end, null).isEmpty()) {
            conflict("Driver is already assigned during this time.");
        }
    }

    private void validateRequest(LocalServiceRunRequest request) {
        if (request == null) badRequest("Local service details are required.");
        if (request.routeId() == null || request.busId() == null || request.driverId() == null) {
            badRequest("Route, bus and driver are required.");
        }
        if (request.serviceDate() == null || request.plannedStartTime() == null) {
            badRequest("Service date and planned start time are required.");
        }
        LocalTime end = request.plannedEndTime();
        if (end == null) {
            badRequest("Planned end time is required.");
        }
        if (!end.isAfter(request.plannedStartTime())) {
            badRequest("Planned end time must be after start time.");
        }
        if (request.serviceDate().isBefore(LocalDate.now())) {
            badRequest("Service date must not be in the past.");
        }
        if (request.notes() != null && request.notes().trim().length() > 1000) {
            badRequest("Notes must not exceed 1000 characters.");
        }
    }

    private void apply(LocalServiceRun run, EligibleLocalResources resources, LocalServiceRunRequest request) {
        run.setRoute(resources.route());
        run.setBus(resources.bus());
        run.setDriver(resources.driver());
        run.setServiceDate(request.serviceDate());
        run.setPlannedStartTime(request.plannedStartTime());
        run.setPlannedEndTime(request.plannedEndTime());
        run.setNotes(request.notes());
    }

    private LocalServiceRunResponse toResponse(LocalServiceRun run) {
        return new LocalServiceRunResponse(
                run.getId(),
                run.getRoute().getId(),
                run.getRoute().getCode(),
                run.getRoute().getName(),
                run.getRoute().getOrigin(),
                run.getRoute().getDestination(),
                run.getBus().getId(),
                run.getBus().getBusNumber(),
                run.getBus().getBusName(),
                run.getBus().getSeatCapacity(),
                run.getDriver().getId(),
                run.getDriver().getUser().getFullName(),
                run.getDriver().getLicenseCategory(),
                run.getServiceDate(),
                run.getPlannedStartTime(),
                run.getPlannedEndTime(),
                run.getStatus().name(),
                run.getNotes(),
                run.getActualStartedAt(),
                run.getActualCompletedAt(),
                run.getCreatedAt(),
                run.getUpdatedAt(),
                orderedStops(run.getRoute())
        );
    }

    private List<RouteStopResponse> orderedStops(Route route) {
        return routeStopRepository.findByRouteIdAndActiveTrueOrderByStopOrderAsc(route.getId())
                .stream()
                .map(stop -> new RouteStopResponse(
                        stop.getId(),
                        stop.getBusStop().getId(),
                        stop.getBusStop().getName(),
                        stop.getBusStop().getLandmark(),
                        stop.getStopOrder(),
                        stop.getEstimatedMinutesFromStart(),
                        stop.getCumulativeFare()
                )).toList();
    }

    private TransportOperator approvedOperator(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Authenticated user not found."));
        if (!"OPERATOR".equalsIgnoreCase(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Operator access is required.");
        }
        TransportOperator operator = operatorRepository.findByUser(user)
                .orElseThrow(() -> notFound("Operator application not found."));
        if (operator.getVerificationStatus() != OperatorVerificationStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Operator application is not approved.");
        }
        return operator;
    }

    private DriverProfile approvedDriver(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Authenticated user not found."));
        if (!"DRIVER".equalsIgnoreCase(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Driver access is required.");
        }
        DriverProfile driver = driverRepository.findByUser(user)
                .orElseThrow(() -> notFound("Driver profile not found."));
        if (!driver.isApproved()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Driver profile is not approved.");
        }
        return driver;
    }

    private LocalServiceRun ownedRun(TransportOperator operator, Long id) {
        return localRunRepository.findByIdAndOperator(id, operator)
                .orElseThrow(() -> notFound("Local service not found."));
    }

    private String appendCancellationNote(String current, String reason) {
        if (reason == null || reason.trim().isBlank()) return current;
        String note = "Cancellation reason: " + reason.trim();
        return current == null || current.isBlank() ? note : current + "\n" + note;
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

    private record EligibleLocalResources(Route route, Bus bus, DriverProfile driver) {}
}
