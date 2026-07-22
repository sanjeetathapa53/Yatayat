package com.yatayat.backend.service;

import com.yatayat.backend.dto.AdminFleetLocationResponse;
import com.yatayat.backend.entity.ScheduledTrip;
import com.yatayat.backend.entity.TripLocation;
import com.yatayat.backend.repository.ScheduledTripRepository;
import com.yatayat.backend.repository.TripLocationRepository;
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
public class AdminLiveFleetService {

    private final ScheduledTripRepository tripRepository;
    private final TripLocationRepository locationRepository;

    public AdminLiveFleetService(
            ScheduledTripRepository tripRepository,
            TripLocationRepository locationRepository
    ) {
        this.tripRepository = tripRepository;
        this.locationRepository = locationRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminFleetLocationResponse> activeFleet() {
        List<ScheduledTrip> trips = tripRepository.findAdminLiveTrips();
        if (trips.isEmpty()) return Collections.emptyList();

        Map<Long, TripLocation> locationsByTripId = locationRepository.findByTripIn(trips).stream()
                .collect(Collectors.toMap(location -> location.getTrip().getId(), Function.identity()));
        return trips.stream()
                .map(trip -> toResponse(trip, locationsByTripId.get(trip.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminFleetLocationResponse trip(Long tripId) {
        ScheduledTrip trip = tripRepository.findByIdForAdminTracking(tripId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found."));
        return toResponse(trip, locationRepository.findByTrip(trip).orElse(null));
    }

    private AdminFleetLocationResponse toResponse(ScheduledTrip trip, TripLocation location) {
        return new AdminFleetLocationResponse(
                trip.getId(),
                trip.getOperator().getId(),
                trip.getOperator().getName(),
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
