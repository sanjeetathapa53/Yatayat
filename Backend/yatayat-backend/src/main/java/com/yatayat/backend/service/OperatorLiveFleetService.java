package com.yatayat.backend.service;

import com.yatayat.backend.dto.OperatorFleetLocationResponse;
import com.yatayat.backend.entity.ScheduledTrip;
import com.yatayat.backend.entity.TransportOperator;
import com.yatayat.backend.entity.TripLocation;
import com.yatayat.backend.entity.TripStatus;
import com.yatayat.backend.entity.User;
import com.yatayat.backend.repository.ScheduledTripRepository;
import com.yatayat.backend.repository.TransportOperatorRepository;
import com.yatayat.backend.repository.TripLocationRepository;
import com.yatayat.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OperatorLiveFleetService {

    private final UserRepository userRepository;
    private final TransportOperatorRepository operatorRepository;
    private final ScheduledTripRepository tripRepository;
    private final TripLocationRepository locationRepository;

    public OperatorLiveFleetService(
            UserRepository userRepository,
            TransportOperatorRepository operatorRepository,
            ScheduledTripRepository tripRepository,
            TripLocationRepository locationRepository
    ) {
        this.userRepository = userRepository;
        this.operatorRepository = operatorRepository;
        this.tripRepository = tripRepository;
        this.locationRepository = locationRepository;
    }

    @Transactional(readOnly = true)
    public List<OperatorFleetLocationResponse> activeFleet(String email) {
        TransportOperator operator = requireApprovedOperator(email);
        List<ScheduledTrip> trips = tripRepository.findOperatorLiveTrips(
                operator, List.of(TripStatus.IN_PROGRESS));
        if (trips.isEmpty()) return Collections.emptyList();

        Map<Long, TripLocation> locationsByTripId = locationRepository.findByTripIn(trips).stream()
                .collect(Collectors.toMap(location -> location.getTrip().getId(), Function.identity()));
        return trips.stream()
                .map(trip -> toResponse(trip, locationsByTripId.get(trip.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public OperatorFleetLocationResponse trip(String email, Long tripId) {
        TransportOperator operator = requireApprovedOperator(email);
        ScheduledTrip trip = tripRepository.findByIdAndOperator(tripId, operator)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found."));
        return toResponse(trip, locationRepository.findByTrip(trip).orElse(null));
    }

    private TransportOperator requireApprovedOperator(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Authenticated operator not found."));
        if (!"OPERATOR".equalsIgnoreCase(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Operator access is required.");
        }
        TransportOperator operator = operatorRepository.findByUser(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Operator application not found."));
        if (!operator.isApproved()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Approved operator access is required.");
        }
        return operator;
    }

    private OperatorFleetLocationResponse toResponse(ScheduledTrip trip, TripLocation location) {
        return new OperatorFleetLocationResponse(
                trip.getId(),
                trip.getBus().getId(),
                trip.getBus().getBusNumber(),
                trip.getBus().getBusName(),
                trip.getDriver().getId(),
                trip.getDriver().getUser().getFullName(),
                trip.getRoute().getId(),
                trip.getRoute().getName(),
                trip.getRoute().getOrigin(),
                trip.getRoute().getDestination(),
                location == null ? null : location.getLatitude(),
                location == null ? null : location.getLongitude(),
                location == null ? null : location.getSpeed(),
                location == null ? null : location.getHeading(),
                location == null ? null : location.getUpdatedAt(),
                trip.getStatus()
        );
    }
}
