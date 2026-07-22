package com.yatayat.backend.trip;

import com.yatayat.backend.dto.AdminFleetLocationResponse;
import com.yatayat.backend.entity.*;
import com.yatayat.backend.repository.ScheduledTripRepository;
import com.yatayat.backend.repository.TripLocationRepository;
import com.yatayat.backend.service.AdminLiveFleetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminLiveFleetServiceTests {

    @Mock private ScheduledTripRepository tripRepository;
    @Mock private TripLocationRepository locationRepository;
    private AdminLiveFleetService service;

    @BeforeEach
    void setUp() {
        service = new AdminLiveFleetService(tripRepository, locationRepository);
    }

    @Test
    void returnsActiveTripsAcrossAllApprovedOperators() {
        ScheduledTrip first = trip(10L, 1L, "Operator One", TripStatus.IN_PROGRESS);
        ScheduledTrip second = trip(11L, 2L, "Operator Two", TripStatus.IN_PROGRESS);
        TripLocation firstLocation = location(first, 27.7, 85.3);
        TripLocation secondLocation = location(second, 27.8, 85.4);
        when(tripRepository.findAdminLiveTrips()).thenReturn(List.of(first, second));
        when(locationRepository.findByTripIn(List.of(first, second)))
                .thenReturn(List.of(firstLocation, secondLocation));

        List<AdminFleetLocationResponse> responses = service.activeFleet();

        assertEquals(2, responses.size());
        assertEquals("Operator One", responses.get(0).operatorName());
        assertEquals("Operator Two", responses.get(1).operatorName());
    }

    @Test
    void missingGpsIsRepresentedWithNullCoordinates() {
        ScheduledTrip trip = trip(12L, 1L, "Operator One", TripStatus.IN_PROGRESS);
        when(tripRepository.findAdminLiveTrips()).thenReturn(List.of(trip));
        when(locationRepository.findByTripIn(List.of(trip))).thenReturn(List.of());

        AdminFleetLocationResponse response = service.activeFleet().get(0);

        assertNull(response.latitude());
        assertNull(response.longitude());
        assertNull(response.updatedAt());
    }

    @Test
    void singleTripLookupReturnsCurrentOrFinalStatus() {
        ScheduledTrip trip = trip(13L, 1L, "Operator One", TripStatus.COMPLETED);
        TripLocation location = location(trip, 27.9, 85.5);
        when(tripRepository.findByIdForAdminTracking(13L)).thenReturn(Optional.of(trip));
        when(locationRepository.findByTrip(trip)).thenReturn(Optional.of(location));

        AdminFleetLocationResponse response = service.trip(13L);

        assertEquals(TripStatus.COMPLETED, response.tripStatus());
        assertEquals(27.9, response.latitude());
    }

    @Test
    void nonexistentTripReturnsNotFound() {
        when(tripRepository.findByIdForAdminTracking(99L)).thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class, () -> service.trip(99L));

        assertEquals(404, error.getStatusCode().value());
    }

    private ScheduledTrip trip(Long tripId, Long operatorId, String operatorName, TripStatus status) {
        TransportOperator operator = new TransportOperator();
        operator.setId(operatorId);
        operator.setName(operatorName);
        Bus bus = new Bus();
        bus.setId(tripId + 100);
        bus.setBusNumber("BA 1 PA " + tripId);
        bus.setBusName("Bus " + tripId);
        User driverUser = new User("Driver " + tripId, "driver@example.com", "9800000000", "encoded", "DRIVER");
        DriverProfile driver = new DriverProfile(driverUser);
        driver.setId(tripId + 200);
        Route route = new Route();
        route.setId(tripId + 300);
        route.setName("Ring Road");
        route.setOrigin("Kalanki");
        route.setDestination("Koteshwor");
        ScheduledTrip trip = new ScheduledTrip();
        trip.setId(tripId);
        trip.setOperator(operator);
        trip.setBus(bus);
        trip.setDriver(driver);
        trip.setRoute(route);
        trip.setStatus(status);
        return trip;
    }

    private TripLocation location(ScheduledTrip trip, double latitude, double longitude) {
        TripLocation location = new TripLocation();
        location.setTrip(trip);
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        location.setSpeed(8.0);
        location.setHeading(45.0);
        location.setUpdatedAt(LocalDateTime.of(2026, 7, 22, 12, 0));
        return location;
    }
}
