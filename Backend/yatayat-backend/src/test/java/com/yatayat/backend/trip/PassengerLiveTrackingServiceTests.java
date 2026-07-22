package com.yatayat.backend.trip;

import com.yatayat.backend.dto.PassengerTripLocationResponse;
import com.yatayat.backend.entity.Bus;
import com.yatayat.backend.entity.Route;
import com.yatayat.backend.entity.ScheduledTrip;
import com.yatayat.backend.entity.TripLocation;
import com.yatayat.backend.entity.TripStatus;
import com.yatayat.backend.repository.ScheduledTripRepository;
import com.yatayat.backend.repository.TripLocationRepository;
import com.yatayat.backend.service.PassengerLiveTrackingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PassengerLiveTrackingServiceTests {

    @Mock private ScheduledTripRepository tripRepository;
    @Mock private TripLocationRepository locationRepository;
    private PassengerLiveTrackingService service;

    @BeforeEach
    void setUp() {
        service = new PassengerLiveTrackingService(tripRepository, locationRepository);
    }

    @Test
    void returnsLatestLocationAndTripDetailsForActiveTrips() {
        ScheduledTrip trip = trip(15L, TripStatus.IN_PROGRESS);
        TripLocation location = location(trip, 27.7, 85.3);
        when(tripRepository.findByStatusOrderByDepartureAtAsc(TripStatus.IN_PROGRESS)).thenReturn(List.of(trip));
        when(locationRepository.findByTripIn(List.of(trip))).thenReturn(List.of(location));

        PassengerTripLocationResponse response = service.activeLocations().get(0);

        assertEquals(15L, response.tripId());
        assertEquals("BA 1 PA 1234", response.bus().number());
        assertEquals("Kathmandu to Pokhara", response.routeName());
        assertEquals(27.7, response.latitude());
        assertEquals(TripStatus.IN_PROGRESS, response.tripStatus());
    }

    @Test
    void activeTripWithoutDriverGpsReturnsDetailsWithNullCoordinates() {
        ScheduledTrip trip = trip(16L, TripStatus.IN_PROGRESS);
        when(tripRepository.findByStatusOrderByDepartureAtAsc(TripStatus.IN_PROGRESS)).thenReturn(List.of(trip));
        when(locationRepository.findByTripIn(List.of(trip))).thenReturn(List.of());

        PassengerTripLocationResponse response = service.activeLocations().get(0);

        assertNull(response.latitude());
        assertNull(response.updatedAt());
    }

    @Test
    void tripSpecificLookupReturnsCompletedStatusForPollingClients() {
        ScheduledTrip trip = trip(17L, TripStatus.COMPLETED);
        TripLocation location = location(trip, 27.8, 85.4);
        when(tripRepository.findByIdForTracking(17L)).thenReturn(Optional.of(trip));
        when(locationRepository.findByTrip(trip)).thenReturn(Optional.of(location));

        PassengerTripLocationResponse response = service.locationForTrip(17L);

        assertEquals(TripStatus.COMPLETED, response.tripStatus());
        assertEquals(27.8, response.latitude());
    }

    private ScheduledTrip trip(Long id, TripStatus status) {
        Bus bus = new Bus();
        bus.setId(5L);
        bus.setBusNumber("BA 1 PA 1234");
        bus.setBusName("Green Line");
        Route route = new Route();
        route.setId(8L);
        route.setName("Kathmandu to Pokhara");
        route.setOrigin("Kathmandu");
        route.setDestination("Pokhara");
        ScheduledTrip trip = new ScheduledTrip();
        trip.setId(id);
        trip.setBus(bus);
        trip.setRoute(route);
        trip.setStatus(status);
        return trip;
    }

    private TripLocation location(ScheduledTrip trip, double latitude, double longitude) {
        TripLocation location = new TripLocation();
        location.setTrip(trip);
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        location.setSpeed(10.0);
        location.setHeading(90.0);
        location.setUpdatedAt(LocalDateTime.of(2026, 7, 22, 12, 0));
        return location;
    }
}
