package com.yatayat.backend.trip;

import com.yatayat.backend.dto.AdminLiveMonitoringResponse;
import com.yatayat.backend.entity.*;
import com.yatayat.backend.repository.*;
import com.yatayat.backend.service.AdminLiveMonitoringService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminLiveMonitoringServiceTests {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 27, 12, 0);
    @Mock private ScheduledTripRepository tripRepository;
    @Mock private TripLocationRepository tripLocationRepository;
    @Mock private LocalServiceRunRepository runRepository;
    @Mock private LocalServiceLocationRepository localLocationRepository;
    private AdminLiveMonitoringService service;

    @BeforeEach
    void setUp() {
        service = new AdminLiveMonitoringService(
                tripRepository, tripLocationRepository, runRepository, localLocationRepository,
                30, 300, 120,
                Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC));
    }

    @Test
    void returnsActiveLocalAndRelevantScheduledOperationsWithLatestLocations() {
        ScheduledTrip trip = trip(10L, TripStatus.IN_PROGRESS);
        LocalServiceRun run = run(20L, LocalServiceRunStatus.IN_SERVICE);
        TripLocation tripLocation = tripLocation(trip, NOW.minusSeconds(10));
        LocalServiceLocation localLocation = localLocation(run, NOW.minusSeconds(90));
        when(tripRepository.findAdminMonitoredTrips(any(), any(), any())).thenReturn(List.of(trip));
        when(runRepository.findAdminMonitoredRuns(LocalServiceRunStatus.IN_SERVICE)).thenReturn(List.of(run));
        when(tripLocationRepository.findByTripIn(List.of(trip))).thenReturn(List.of(tripLocation));
        when(localLocationRepository.findByRunIn(List.of(run))).thenReturn(List.of(localLocation));

        AdminLiveMonitoringResponse response = service.snapshot();

        assertThat(response.vehicles()).hasSize(2);
        assertThat(response.vehicles().get(0))
                .extracting(AdminLiveMonitoringResponse.Vehicle::operationType,
                        AdminLiveMonitoringResponse.Vehicle::locationFreshness,
                        AdminLiveMonitoringResponse.Vehicle::lastGpsUpdateAgeSeconds)
                .containsExactly("SCHEDULED_TRIP", "LIVE", 10L);
        assertThat(response.vehicles().get(1))
                .extracting(AdminLiveMonitoringResponse.Vehicle::operationType,
                        AdminLiveMonitoringResponse.Vehicle::locationFreshness)
                .containsExactly("LOCAL_SERVICE", "STALE");
        verify(tripLocationRepository).findByTripIn(List.of(trip));
        verify(localLocationRepository).findByRunIn(List.of(run));
    }

    @Test
    void missingAndExpiredGpsAreOffline() {
        ScheduledTrip missing = trip(11L, TripStatus.BOARDING);
        LocalServiceRun old = run(21L, LocalServiceRunStatus.IN_SERVICE);
        when(tripRepository.findAdminMonitoredTrips(any(), any(), any())).thenReturn(List.of(missing));
        when(runRepository.findAdminMonitoredRuns(LocalServiceRunStatus.IN_SERVICE)).thenReturn(List.of(old));
        when(tripLocationRepository.findByTripIn(List.of(missing))).thenReturn(List.of());
        when(localLocationRepository.findByRunIn(List.of(old)))
                .thenReturn(List.of(localLocation(old, NOW.minusMinutes(6))));

        AdminLiveMonitoringResponse response = service.snapshot();

        assertThat(response.vehicles())
                .extracting(AdminLiveMonitoringResponse.Vehicle::locationFreshness)
                .containsExactly("OFFLINE", "OFFLINE");
        assertThat(response.vehicles().get(0).lastGpsUpdateAgeSeconds()).isNull();
    }

    @Test
    void requestsOnlyActiveStatusesAndBoundedUpcomingTrips() {
        when(tripRepository.findAdminMonitoredTrips(any(), any(), any())).thenReturn(List.of());
        when(runRepository.findAdminMonitoredRuns(any())).thenReturn(List.of());

        service.snapshot();

        verify(tripRepository).findAdminMonitoredTrips(
                eq(NOW), eq(NOW.plusMinutes(120)),
                eq(List.of(TripStatus.BOARDING, TripStatus.IN_PROGRESS)));
        verify(runRepository).findAdminMonitoredRuns(LocalServiceRunStatus.IN_SERVICE);
    }

    private ScheduledTrip trip(Long id, TripStatus status) {
        Fixture fixture = fixture(id);
        ScheduledTrip value = new ScheduledTrip();
        value.setId(id); value.setBus(fixture.bus); value.setOperator(fixture.operator);
        value.setDriver(fixture.driver); value.setRoute(fixture.route); value.setStatus(status);
        return value;
    }

    private LocalServiceRun run(Long id, LocalServiceRunStatus status) {
        Fixture fixture = fixture(id);
        LocalServiceRun value = new LocalServiceRun();
        value.setId(id); value.setBus(fixture.bus); value.setOperator(fixture.operator);
        value.setDriver(fixture.driver); value.setRoute(fixture.route); value.setStatus(status);
        return value;
    }

    private Fixture fixture(Long id) {
        TransportOperator operator = new TransportOperator();
        operator.setId(id + 100); operator.setName("Operator " + id);
        Bus bus = new Bus();
        bus.setId(id + 200); bus.setBusNumber("BA 1 PA " + id);
        bus.setBusName("Bus " + id); bus.setStatus(BusStatus.APPROVED);
        User user = new User("Driver " + id, "driver-" + id + "@example.com",
                "9800000000", "encoded", "DRIVER");
        DriverProfile driver = new DriverProfile(user);
        driver.setId(id + 300); driver.setVerificationStatus(DriverVerificationStatus.APPROVED);
        driver.setLicenseExpiryDate(NOW.toLocalDate().plusYears(1));
        Route route = new Route();
        route.setId(id + 400); route.setName("Route " + id);
        route.setOrigin("Origin"); route.setDestination("Destination");
        return new Fixture(operator, bus, driver, route);
    }

    private TripLocation tripLocation(ScheduledTrip trip, LocalDateTime at) {
        TripLocation value = new TripLocation();
        value.setTrip(trip); value.setLatitude(27.7); value.setLongitude(85.3); value.setUpdatedAt(at);
        return value;
    }

    private LocalServiceLocation localLocation(LocalServiceRun run, LocalDateTime at) {
        LocalServiceLocation value = new LocalServiceLocation();
        value.setRun(run); value.setLatitude(27.8); value.setLongitude(85.4); value.setUpdatedAt(at);
        return value;
    }

    private record Fixture(TransportOperator operator, Bus bus, DriverProfile driver, Route route) {}
}
