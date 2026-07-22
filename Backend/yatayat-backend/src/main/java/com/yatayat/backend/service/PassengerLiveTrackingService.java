package com.yatayat.backend.service;

import com.yatayat.backend.dto.PassengerTripLocationResponse;
import com.yatayat.backend.entity.Bus;
import com.yatayat.backend.entity.ScheduledTrip;
import com.yatayat.backend.entity.TripLocation;
import com.yatayat.backend.entity.TripStatus;
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
public class PassengerLiveTrackingService {

    private final ScheduledTripRepository tripRepository;
    private final TripLocationRepository locationRepository;

    public PassengerLiveTrackingService(
            ScheduledTripRepository tripRepository,
            TripLocationRepository locationRepository
    ) {
        this.tripRepository = tripRepository;
        this.locationRepository = locationRepository;
    }

    @Transactional(readOnly = true)
    public List<PassengerTripLocationResponse> activeLocations() {
        List<ScheduledTrip> trips = tripRepository.findByStatusOrderByDepartureAtAsc(TripStatus.IN_PROGRESS);
        if (trips.isEmpty()) return Collections.emptyList();

        Map<Long, TripLocation> locationsByTripId = locationRepository.findByTripIn(trips).stream()
                .collect(Collectors.toMap(location -> location.getTrip().getId(), Function.identity()));

        return trips.stream()
                .map(trip -> toResponse(trip, locationsByTripId.get(trip.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public PassengerTripLocationResponse locationForTrip(Long tripId) {
        ScheduledTrip trip = tripRepository.findByIdForTracking(tripId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found."));
        TripLocation location = locationRepository.findByTrip(trip).orElse(null);
        return toResponse(trip, location);
    }

    private PassengerTripLocationResponse toResponse(ScheduledTrip trip, TripLocation location) {
        Bus bus = trip.getBus();
        return new PassengerTripLocationResponse(
                trip.getId(),
                bus == null ? null : new PassengerTripLocationResponse.BusInfo(
                        bus.getId(), bus.getBusNumber(), bus.getBusName()),
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
