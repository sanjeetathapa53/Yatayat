package com.yatayat.backend.service;

import com.yatayat.backend.dto.RouteRequest;
import com.yatayat.backend.dto.RouteResponse;
import com.yatayat.backend.entity.*;
import com.yatayat.backend.repository.RouteRepository;
import com.yatayat.backend.repository.TransportOperatorRepository;
import com.yatayat.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class RouteService {

    private final RouteRepository routeRepository;
    private final UserRepository userRepository;
    private final TransportOperatorRepository operatorRepository;

    public RouteService(
            RouteRepository routeRepository,
            UserRepository userRepository,
            TransportOperatorRepository operatorRepository
    ) {
        this.routeRepository = routeRepository;
        this.userRepository = userRepository;
        this.operatorRepository = operatorRepository;
    }

    public List<RouteResponse> getAllRoutes() {
        return routeRepository.findAllByOrderByCodeAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    public RouteResponse getRoute(Long id) {
        return toResponse(findRoute(id));
    }

    @Transactional
    public RouteResponse createRoute(RouteRequest request) {
        validate(request);
        String code = request.code().trim();
        if (routeRepository.existsByCodeIgnoreCase(code)) {
            conflict("Route code is already registered");
        }

        Route route = new Route();
        apply(route, request);
        try {
            return toResponse(routeRepository.saveAndFlush(route));
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Route code is already registered"
            );
        }
    }

    @Transactional
    public RouteResponse updateRoute(Long id, RouteRequest request) {
        validate(request);
        Route route = findRoute(id);
        if (routeRepository.existsByCodeIgnoreCaseAndIdNot(
                request.code().trim(), id
        )) {
            conflict("Route code is already registered");
        }
        apply(route, request);
        try {
            return toResponse(routeRepository.saveAndFlush(route));
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Route code is already registered"
            );
        }
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

    private void apply(Route route, RouteRequest request) {
        route.setCode(request.code());
        route.setName(request.name());
        route.setOrigin(request.origin());
        route.setDestination(request.destination());
        route.setDistanceKm(request.distanceKm());
        route.setEstimatedDurationMinutes(request.estimatedDurationMinutes());
        route.setStatus(request.status() == null ? RouteStatus.ACTIVE : request.status());
    }

    private void validate(RouteRequest request) {
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
        if (request.origin().trim().equalsIgnoreCase(request.destination().trim())) {
            badRequest("Origin and destination must be different");
        }
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
        return new RouteResponse(
                route.getId(), route.getCode(), route.getName(),
                route.getOrigin(), route.getDestination(), route.getDistanceKm(),
                route.getEstimatedDurationMinutes(), route.getStatus().name(),
                route.getCreatedAt(), route.getUpdatedAt()
        );
    }
}
