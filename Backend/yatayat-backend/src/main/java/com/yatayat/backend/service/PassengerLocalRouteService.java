package com.yatayat.backend.service;

import com.yatayat.backend.dto.PassengerLocalRouteResponse;
import com.yatayat.backend.dto.RouteStopResponse;
import com.yatayat.backend.entity.*;
import com.yatayat.backend.repository.RouteRepository;
import com.yatayat.backend.repository.RouteStopRepository;
import com.yatayat.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class PassengerLocalRouteService {
    private final UserRepository userRepository;
    private final RouteRepository routeRepository;
    private final RouteStopRepository routeStopRepository;

    public PassengerLocalRouteService(UserRepository userRepository, RouteRepository routeRepository,
                                      RouteStopRepository routeStopRepository) {
        this.userRepository = userRepository;
        this.routeRepository = routeRepository;
        this.routeStopRepository = routeStopRepository;
    }

    public List<PassengerLocalRouteResponse> search(String email, String origin, String destination) {
        requirePassenger(email);
        String from = required(origin, "Origin is required.");
        String to = required(destination, "Destination is required.");
        if (from.equalsIgnoreCase(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Origin and destination must be different.");
        }
        return routeRepository.findByStatusAndTripTypeOrderByCodeAsc(RouteStatus.ACTIVE, TripType.LOCAL)
                .stream()
                .filter(route -> route.getStatus() == RouteStatus.ACTIVE)
                .filter(route -> route.getTripType() == TripType.LOCAL)
                .map(route -> matchRoute(route, from, to))
                .flatMap(Optional::stream)
                .toList();
    }

    public List<PassengerLocalRouteResponse> searchByStopIds(String email, Long fromStopId, Long toStopId) {
        requirePassenger(email);
        if (fromStopId == null || toStopId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Boarding and destination stops are required.");
        }
        if (fromStopId.equals(toStopId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Boarding and destination stops must be different.");
        }
        return routeRepository.findByStatusAndTripTypeOrderByCodeAsc(RouteStatus.ACTIVE, TripType.LOCAL)
                .stream()
                .filter(route -> route.getStatus() == RouteStatus.ACTIVE)
                .filter(route -> route.getTripType() == TripType.LOCAL)
                .map(route -> matchRoute(route, fromStopId, toStopId))
                .flatMap(Optional::stream)
                .toList();
    }

    public PassengerLocalRouteResponse details(String email, Long routeId) {
        requirePassenger(email);
        return routeRepository.findByIdAndStatusAndTripType(routeId, RouteStatus.ACTIVE, TripType.LOCAL)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Local route not found."));
    }

    private User requirePassenger(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Authenticated user not found"));
        if (!"PASSENGER".equalsIgnoreCase(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Passenger access is required");
        }
        return user;
    }

    private String required(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return value.trim();
    }

    private PassengerLocalRouteResponse toResponse(Route route) {
        List<RouteStop> stops = activeStops(route);
        if (stops.isEmpty()) {
            return legacyResponse(route);
        }
        return buildResponse(route, stops, stops.get(0), stops.get(stops.size() - 1));
    }

    private Optional<PassengerLocalRouteResponse> matchRoute(Route route, String from, String to) {
        List<RouteStop> stops = activeStops(route);
        if (stops.isEmpty()) {
            boolean endpointMatch = route.getOrigin().equalsIgnoreCase(from) && route.getDestination().equalsIgnoreCase(to);
            return endpointMatch ? Optional.of(legacyResponse(route)) : Optional.empty();
        }
        RouteStop boarding = null;
        RouteStop destination = null;
        for (RouteStop stop : stops) {
            if (boarding == null && matches(stop.getBusStop(), from)) boarding = stop;
            if (boarding != null && stop.getStopOrder() > boarding.getStopOrder() && matches(stop.getBusStop(), to)) {
                destination = stop;
                break;
            }
        }
        return boarding != null && destination != null
                ? Optional.of(buildResponse(route, stops, boarding, destination))
                : Optional.empty();
    }

    private Optional<PassengerLocalRouteResponse> matchRoute(Route route, Long fromStopId, Long toStopId) {
        List<RouteStop> stops = activeStops(route);
        RouteStop boarding = null;
        RouteStop destination = null;
        for (RouteStop stop : stops) {
            if (boarding == null && stop.getBusStop().getId().equals(fromStopId)) boarding = stop;
            if (boarding != null && stop.getStopOrder() > boarding.getStopOrder()
                    && stop.getBusStop().getId().equals(toStopId)) {
                destination = stop;
                break;
            }
        }
        return boarding != null && destination != null
                ? Optional.of(buildResponse(route, stops, boarding, destination))
                : Optional.empty();
    }

    private List<RouteStop> activeStops(Route route) {
        return routeStopRepository.findByRouteIdAndActiveTrueOrderByStopOrderAsc(route.getId()).stream()
                .filter(stop -> stop.getBusStop().isActive())
                .toList();
    }

    private boolean matches(BusStop stop, String query) {
        return stop.getName().equalsIgnoreCase(query)
                || (stop.getLandmark() != null && stop.getLandmark().equalsIgnoreCase(query));
    }

    private PassengerLocalRouteResponse buildResponse(Route route, List<RouteStop> stops,
                                                      RouteStop boarding, RouteStop destination) {
        BigDecimal fare = destination.getCumulativeFare().subtract(boarding.getCumulativeFare());
        int duration = destination.getEstimatedMinutesFromStart() - boarding.getEstimatedMinutesFromStart();
        int intermediateStops = Math.max(0, destination.getStopOrder() - boarding.getStopOrder() - 1);
        return new PassengerLocalRouteResponse(
                route.getId(), route.getCode(), route.getName(), route.getOrigin(), route.getDestination(),
                route.getDistanceKm(), route.getEstimatedDurationMinutes(), route.getTripType().name(),
                stops.stream().map(stop -> stop.getBusStop().getName()).toList(),
                "Estimated fare: NPR " + fare,
                boarding.getBusStop().getId(), boarding.getBusStop().getName(),
                destination.getBusStop().getId(), destination.getBusStop().getName(),
                fare, duration,
                route.getOperatingStartTime() == null ? null : route.getOperatingStartTime().toString(),
                route.getOperatingEndTime() == null ? null : route.getOperatingEndTime().toString(),
                intermediateStops,
                stops.stream().map(this::toRouteStopResponse).toList()
        );
    }

    private PassengerLocalRouteResponse legacyResponse(Route route) {
        return new PassengerLocalRouteResponse(
                route.getId(), route.getCode(), route.getName(), route.getOrigin(), route.getDestination(),
                route.getDistanceKm(), route.getEstimatedDurationMinutes(), route.getTripType().name(),
                List.of(route.getOrigin(), route.getDestination()), "Fare information is not available.",
                null, route.getOrigin(), null, route.getDestination(), null, route.getEstimatedDurationMinutes(),
                route.getOperatingStartTime() == null ? null : route.getOperatingStartTime().toString(),
                route.getOperatingEndTime() == null ? null : route.getOperatingEndTime().toString(),
                0, List.of()
        );
    }

    private RouteStopResponse toRouteStopResponse(RouteStop routeStop) {
        BusStop stop = routeStop.getBusStop();
        return new RouteStopResponse(routeStop.getId(), stop.getId(), stop.getName(), stop.getLandmark(),
                routeStop.getStopOrder(), routeStop.getEstimatedMinutesFromStart(), routeStop.getCumulativeFare());
    }
}
