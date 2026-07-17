package com.yatayat.backend.service;

import com.yatayat.backend.dto.PassengerLocalRouteResponse;
import com.yatayat.backend.entity.*;
import com.yatayat.backend.repository.RouteRepository;
import com.yatayat.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class PassengerLocalRouteService {
    private final UserRepository userRepository;
    private final RouteRepository routeRepository;

    public PassengerLocalRouteService(UserRepository userRepository, RouteRepository routeRepository) {
        this.userRepository = userRepository;
        this.routeRepository = routeRepository;
    }

    public List<PassengerLocalRouteResponse> search(String email, String origin, String destination) {
        requirePassenger(email);
        String from = required(origin, "Origin is required.");
        String to = required(destination, "Destination is required.");
        if (from.equalsIgnoreCase(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Origin and destination must be different.");
        }
        return routeRepository
                .findByStatusAndTripTypeAndOriginIgnoreCaseAndDestinationIgnoreCaseOrderByCodeAsc(
                        RouteStatus.ACTIVE, TripType.LOCAL, from, to)
                .stream()
                .filter(route -> route.getStatus() == RouteStatus.ACTIVE)
                .filter(route -> route.getTripType() == TripType.LOCAL)
                .map(this::toResponse).toList();
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
        return new PassengerLocalRouteResponse(
                route.getId(), route.getCode(), route.getName(), route.getOrigin(), route.getDestination(),
                route.getDistanceKm(), route.getEstimatedDurationMinutes(), route.getTripType().name(),
                List.of(route.getOrigin(), route.getDestination()), "Fare information is not available."
        );
    }
}
