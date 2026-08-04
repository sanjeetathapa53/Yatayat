package com.yatayat.backend.service;

import com.yatayat.backend.dto.RouteRequest;
import com.yatayat.backend.dto.RouteResponse;
import com.yatayat.backend.dto.RouteStopRequest;
import com.yatayat.backend.dto.RouteStopResponse;
import com.yatayat.backend.entity.*;
import com.yatayat.backend.repository.BusStopRepository;
import com.yatayat.backend.repository.RouteRepository;
import com.yatayat.backend.repository.RouteStopRepository;
import com.yatayat.backend.repository.TransportOperatorRepository;
import com.yatayat.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class RouteService {

    private final RouteRepository routeRepository;
    private final BusStopRepository busStopRepository;
    private final RouteStopRepository routeStopRepository;
    private final UserRepository userRepository;
    private final TransportOperatorRepository operatorRepository;

    public RouteService(
            RouteRepository routeRepository,
            BusStopRepository busStopRepository,
            RouteStopRepository routeStopRepository,
            UserRepository userRepository,
            TransportOperatorRepository operatorRepository
    ) {
        this.routeRepository = routeRepository;
        this.busStopRepository = busStopRepository;
        this.routeStopRepository = routeStopRepository;
        this.userRepository = userRepository;
        this.operatorRepository = operatorRepository;
    }

    public List<RouteResponse> getAllRoutes() {
        return getRoutes(null, null, null);
    }

    public List<RouteResponse> getRoutes(TripType type, Boolean active, String search) {
        RouteStatus requestedStatus = active == null
                ? null
                : active ? RouteStatus.ACTIVE : RouteStatus.INACTIVE;
        String normalizedSearch = search == null
                ? ""
                : search.trim().toLowerCase(Locale.ROOT);

        return routeRepository.findAllByOrderByCodeAsc().stream()
                .filter(route -> type == null || route.getTripType() == type)
                .filter(route -> requestedStatus == null || route.getStatus() == requestedStatus)
                .filter(route -> normalizedSearch.isEmpty() || matchesSearch(route, normalizedSearch))
                .map(this::toResponse)
                .toList();
    }

    public RouteResponse getRoute(Long id) {
        return toResponse(findRoute(id));
    }

    public List<RouteResponse> getLocalRoutes() {
        return routeRepository.findByTripTypeOrderByCodeAsc(TripType.LOCAL).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public RouteResponse createRoute(RouteRequest request) {
        validate(request, true);
        String code = request.code().trim();
        if (routeRepository.existsByCodeIgnoreCase(code)) {
            conflict("Route code is already registered");
        }

        Route route = new Route();
        apply(route, request);
        try {
            Route saved = routeRepository.saveAndFlush(route);
            if (saved.getTripType() == TripType.LOCAL && request.stops() != null) {
                replaceRouteStops(saved, request.stops());
            }
            return toResponse(saved);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Route code is already registered"
            );
        }
    }

    @Transactional
    public RouteResponse updateRoute(Long id, RouteRequest request) {
        validate(request, false);
        Route route = findRoute(id);
        if (routeRepository.existsByCodeIgnoreCaseAndIdNot(
                request.code().trim(), id
        )) {
            conflict("Route code is already registered");
        }
        apply(route, request);
        try {
            Route saved = routeRepository.saveAndFlush(route);
            if (saved.getTripType() == TripType.LOCAL && request.stops() != null) {
                replaceRouteStops(saved, request.stops());
            }
            return toResponse(saved);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Route code is already registered"
            );
        }
    }

    @Transactional
    public RouteResponse createLocalRoute(RouteRequest request) {
        RouteRequest localRequest = new RouteRequest(
                request.code(), request.name(), request.origin(), request.destination(), request.distanceKm(),
                request.estimatedDurationMinutes(), request.operatingStartTime(), request.operatingEndTime(),
                TripType.LOCAL, request.status(), request.stops()
        );
        if (localRequest.stops() == null || localRequest.stops().size() < 2) {
            badRequest("Local route must include at least two ordered stops");
        }
        return createRoute(localRequest);
    }

    @Transactional
    public RouteResponse replaceRouteStops(Long routeId, List<RouteStopRequest> stops) {
        Route route = findRoute(routeId);
        if (route.getTripType() != TripType.LOCAL) {
            badRequest("Ordered stops can only be managed for local routes");
        }
        replaceRouteStops(route, stops);
        return toResponse(route);
    }

    @Transactional
    public RouteResponse setStatus(Long routeId, RouteStatus status) {
        if (status == null) badRequest("Route status is required");
        Route route = findRoute(routeId);
        route.setStatus(status);
        return toResponse(routeRepository.saveAndFlush(route));
    }

    public List<RouteResponse> getActiveRoutesForOperator(String email) {
        requireApprovedOperator(email);
        return routeRepository.findByStatusOrderByCodeAsc(RouteStatus.ACTIVE)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private void requireApprovedOperator(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Authenticated user not found"
                ));
        TransportOperator operator = operatorRepository.findByUser(user)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Operator application not found"
                ));
        if (!"OPERATOR".equalsIgnoreCase(user.getRole()) ||
                operator.getVerificationStatus() != OperatorVerificationStatus.APPROVED) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Approved operator access is required"
            );
        }
    }

    private Route findRoute(Long id) {
        return routeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Route not found"
                ));
    }

    private boolean matchesSearch(Route route, String search) {
        return contains(route.getCode(), search) ||
                contains(route.getName(), search) ||
                contains(route.getOrigin(), search) ||
                contains(route.getDestination(), search);
    }

    private boolean contains(String value, String search) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(search);
    }

    private void apply(Route route, RouteRequest request) {
        route.setCode(request.code());
        route.setName(request.name());
        route.setOrigin(request.origin());
        route.setDestination(request.destination());
        route.setDistanceKm(request.distanceKm());
        route.setEstimatedDurationMinutes(request.estimatedDurationMinutes());
        route.setOperatingStartTime(request.operatingStartTime());
        route.setOperatingEndTime(request.operatingEndTime());
        if (request.tripType() != null) route.setTripType(request.tripType());
        route.setStatus(request.status() == null ? RouteStatus.ACTIVE : request.status());
    }

    private void validate(RouteRequest request, boolean creating) {
        if (request == null) badRequest("Route details are required");
        required(request.code(), "Route code");
        required(request.name(), "Route name");
        required(request.origin(), "Origin");
        required(request.destination(), "Destination");
        maximum(request.code(), 40, "Route code");
        maximum(request.name(), 160, "Route name");
        maximum(request.origin(), 160, "Origin");
        maximum(request.destination(), 160, "Destination");
        if (request.distanceKm() == null || request.distanceKm().signum() <= 0) {
            badRequest("Distance must be greater than zero");
        }
        if (request.estimatedDurationMinutes() == null ||
                request.estimatedDurationMinutes() <= 0) {
            badRequest("Estimated duration must be greater than zero");
        }
        if (creating && request.tripType() == null) {
            badRequest("Trip type is required");
        }
        if (request.origin().trim().equalsIgnoreCase(request.destination().trim())) {
            badRequest("Origin and destination must be different");
        }
        if ((request.operatingStartTime() == null) != (request.operatingEndTime() == null)) {
            badRequest("Both operating start and end time are required when operating hours are set");
        }
    }

    private void replaceRouteStops(Route route, List<RouteStopRequest> stops) {
        List<RouteStopRequest> orderedStops = validateRouteStops(route, stops);
        routeStopRepository.deleteByRouteId(route.getId());
        routeStopRepository.flush();
        List<RouteStop> entities = orderedStops.stream().map(stopRequest -> {
            BusStop stop = busStopRepository.findById(stopRequest.busStopId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bus stop not found"));
            if (!stop.isActive()) {
                badRequest("Inactive stops cannot be assigned to an active local route");
            }
            RouteStop routeStop = new RouteStop();
            routeStop.setRoute(route);
            routeStop.setBusStop(stop);
            routeStop.setStopOrder(stopRequest.stopOrder());
            routeStop.setEstimatedMinutesFromStart(stopRequest.estimatedMinutesFromStart());
            routeStop.setCumulativeFare(stopRequest.cumulativeFare());
            routeStop.setActive(true);
            return routeStop;
        }).toList();
        routeStopRepository.saveAllAndFlush(entities);
    }

    private List<RouteStopRequest> validateRouteStops(Route route, List<RouteStopRequest> stops) {
        if (route.getTripType() != TripType.LOCAL) {
            badRequest("Ordered stops can only be managed for local routes");
        }
        if (stops == null || stops.size() < 2) {
            badRequest("Local route must include at least two ordered stops");
        }
        Set<Integer> orders = new HashSet<>();
        Set<Long> stopIds = new HashSet<>();
        List<RouteStopRequest> ordered = stops.stream()
                .sorted(Comparator.comparing(RouteStopRequest::stopOrder, Comparator.nullsLast(Integer::compareTo)))
                .toList();
        BigDecimal previousFare = null;
        Integer previousMinutes = null;
        for (int index = 0; index < ordered.size(); index++) {
            RouteStopRequest stop = ordered.get(index);
            if (stop.busStopId() == null) badRequest("Bus stop is required");
            if (stop.stopOrder() == null || stop.stopOrder() != index + 1) {
                badRequest("Route stop order must start at 1 and be consecutive");
            }
            if (!orders.add(stop.stopOrder())) badRequest("Duplicate stop order is not allowed");
            if (!stopIds.add(stop.busStopId())) badRequest("Duplicate stops are not allowed on a local route");
            if (stop.estimatedMinutesFromStart() == null || stop.estimatedMinutesFromStart() < 0) {
                badRequest("Estimated minutes from start cannot be negative");
            }
            if (stop.cumulativeFare() == null || stop.cumulativeFare().signum() < 0) {
                badRequest("Cumulative fare cannot be negative");
            }
            if (previousMinutes != null && stop.estimatedMinutesFromStart() < previousMinutes) {
                badRequest("Estimated minutes must not decrease along the route");
            }
            if (previousFare != null && stop.cumulativeFare().compareTo(previousFare) < 0) {
                badRequest("Cumulative fare must not decrease along the route");
            }
            previousMinutes = stop.estimatedMinutesFromStart();
            previousFare = stop.cumulativeFare();
        }
        return ordered;
    }

    private void required(String value, String field) {
        if (value == null || value.isBlank()) badRequest(field + " is required");
    }

    private void maximum(String value, int length, String field) {
        if (value != null && value.trim().length() > length) {
            badRequest(field + " must not exceed " + length + " characters");
        }
    }

    private void badRequest(String message) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private void conflict(String message) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, message);
    }

    private RouteResponse toResponse(Route route) {
        List<RouteStopResponse> stops = route.getTripType() == TripType.LOCAL
                ? routeStopRepository.findByRouteIdOrderByStopOrderAsc(route.getId()).stream().map(this::toRouteStopResponse).toList()
                : List.of();
        return new RouteResponse(
                route.getId(), route.getCode(), route.getName(),
                route.getOrigin(), route.getDestination(), route.getDistanceKm(),
                route.getEstimatedDurationMinutes(), route.getOperatingStartTime(), route.getOperatingEndTime(),
                route.getTripType().name(), route.getStatus().name(), stops,
                route.getCreatedAt(), route.getUpdatedAt()
        );
    }

    private RouteStopResponse toRouteStopResponse(RouteStop routeStop) {
        BusStop stop = routeStop.getBusStop();
        return new RouteStopResponse(routeStop.getId(), stop.getId(), stop.getName(), stop.getLandmark(),
                routeStop.getStopOrder(), routeStop.getEstimatedMinutesFromStart(), routeStop.getCumulativeFare());
    }
}
