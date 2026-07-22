package com.yatayat.backend.trip;

import com.yatayat.backend.dto.OperatorFleetLocationResponse;
import com.yatayat.backend.entity.*;
import com.yatayat.backend.repository.ScheduledTripRepository;
import com.yatayat.backend.repository.TransportOperatorRepository;
import com.yatayat.backend.repository.TripLocationRepository;
import com.yatayat.backend.repository.UserRepository;
import com.yatayat.backend.service.OperatorLiveFleetService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperatorLiveFleetServiceTests {

    @Mock private UserRepository userRepository;
    @Mock private TransportOperatorRepository operatorRepository;
    @Mock private ScheduledTripRepository tripRepository;
    @Mock private TripLocationRepository locationRepository;
    private OperatorLiveFleetService service;
    private User user;
    private TransportOperator operator;

    @BeforeEach
    void setUp() {
        service = new OperatorLiveFleetService(
                userRepository, operatorRepository, tripRepository, locationRepository);
        user = new User("Operator", "operator@example.com", "9800000000", "encoded", "OPERATOR");
        user.setId(1L);
        operator = new TransportOperator();
        operator.setId(2L);
        operator.setUser(user);
        operator.setVerificationStatus(OperatorVerificationStatus.APPROVED);
        when(userRepository.findByEmailIgnoreCase("operator@example.com")).thenReturn(Optional.of(user));
        when(operatorRepository.findByUser(user)).thenReturn(Optional.of(operator));
    }

    @Test
    void activeFleetIsAlwaysScopedToAuthenticatedOperator() {
        ScheduledTrip trip = trip(10L, TripStatus.IN_PROGRESS);
        TripLocation location = location(trip);
        when(tripRepository.findOperatorLiveTrips(operator, List.of(TripStatus.IN_PROGRESS)))
                .thenReturn(List.of(trip));
        when(locationRepository.findByTripIn(List.of(trip))).thenReturn(List.of(location));

        OperatorFleetLocationResponse response = service.activeFleet("operator@example.com").get(0);

        assertEquals(10L, response.tripId());
        assertEquals("BA 1 PA 1234", response.busNumber());
        assertEquals("Driver One", response.driverName());
        assertEquals(27.7, response.latitude());
        verify(tripRepository).findOperatorLiveTrips(operator, List.of(TripStatus.IN_PROGRESS));
    }

    @Test
    void activeTripWithoutGpsReturnsNullCoordinates() {
        ScheduledTrip trip = trip(11L, TripStatus.IN_PROGRESS);
        when(tripRepository.findOperatorLiveTrips(operator, List.of(TripStatus.IN_PROGRESS)))
                .thenReturn(List.of(trip));
        when(locationRepository.findByTripIn(List.of(trip))).thenReturn(List.of());

        OperatorFleetLocationResponse response = service.activeFleet("operator@example.com").get(0);

        assertNull(response.latitude());
        assertNull(response.updatedAt());
    }

    @Test
    void tripLookupCannotReturnAnotherOperatorsTrip() {
        when(tripRepository.findByIdAndOperator(99L, operator)).thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.trip("operator@example.com", 99L));

        assertEquals(404, error.getStatusCode().value());
    }

    private ScheduledTrip trip(Long id, TripStatus status) {
        Bus bus = new Bus();
        bus.setId(3L);
        bus.setBusNumber("BA 1 PA 1234");
        bus.setBusName("Green Line");
        User driverUser = new User("Driver One", "driver@example.com", "9800000001", "encoded", "DRIVER");
        DriverProfile driver = new DriverProfile(driverUser);
        driver.setId(4L);
        Route route = new Route();
        route.setId(5L);
        route.setName("Ring Road");
        route.setOrigin("Kalanki");
        route.setDestination("Koteshwor");
        ScheduledTrip trip = new ScheduledTrip();
        trip.setId(id);
        trip.setOperator(operator);
        trip.setBus(bus);
        trip.setDriver(driver);
        trip.setRoute(route);
        trip.setStatus(status);
        return trip;
    }

    private TripLocation location(ScheduledTrip trip) {
        TripLocation location = new TripLocation();
        location.setTrip(trip);
        location.setLatitude(27.7);
        location.setLongitude(85.3);
        location.setSpeed(8.0);
        location.setHeading(45.0);
        location.setUpdatedAt(LocalDateTime.of(2026, 7, 22, 12, 0));
        return location;
    }
}
