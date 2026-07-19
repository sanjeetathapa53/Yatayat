package com.yatayat.backend.route;

import com.yatayat.backend.dto.BusStopRequest;
import com.yatayat.backend.dto.RouteStopRequest;
import com.yatayat.backend.entity.*;
import com.yatayat.backend.repository.BusStopRepository;
import com.yatayat.backend.repository.RouteRepository;
import com.yatayat.backend.repository.RouteStopRepository;
import com.yatayat.backend.repository.UserRepository;
import com.yatayat.backend.service.BusStopService;
import com.yatayat.backend.service.PassengerLocalRouteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class LocalTravelFoundationServiceTests {
    private BusStopRepository busStops;
    private RouteRepository routes;
    private RouteStopRepository routeStops;
    private UserRepository users;

    @BeforeEach
    void setUp() {
        busStops = mock(BusStopRepository.class);
        routes = mock(RouteRepository.class);
        routeStops = mock(RouteStopRepository.class);
        users = mock(UserRepository.class);
    }

    @Test
    void duplicateBusStopIsRejected() {
        BusStopService service = new BusStopService(busStops);
        when(busStops.existsByNormalizedName("RATNAPARK")).thenReturn(true);

        assertThatThrownBy(() -> service.createStop(new BusStopRequest("Ratnapark", null, null, "Near old bus park", true)))
                .isInstanceOfSatisfying(ResponseStatusException.class, error -> assertThat(error.getStatusCode().value()).isEqualTo(409));
    }

    @Test
    void invalidCoordinatesAreRejected() {
        BusStopService service = new BusStopService(busStops);

        assertThatThrownBy(() -> service.createStop(new BusStopRequest("Bad Stop", new BigDecimal("91"), null, null, true)))
                .isInstanceOfSatisfying(ResponseStatusException.class, error -> assertThat(error.getStatusCode().value()).isEqualTo(400));
    }

    @Test
    void passengerDirectRouteSearchCalculatesFareAndDuration() {
        PassengerLocalRouteService service = new PassengerLocalRouteService(users, routes, routeStops);
        User passenger = new User("Passenger", "passenger@test.local", "9800000000", "encoded", "PASSENGER");
        when(users.findByEmailIgnoreCase("passenger@test.local")).thenReturn(Optional.of(passenger));
        Route route = localRoute();
        BusStop stop1 = stop(1L, "Gongabu", true);
        BusStop stop2 = stop(2L, "Balaju", true);
        BusStop stop3 = stop(3L, "Ratnapark", true);
        when(routes.findByStatusAndTripTypeOrderByCodeAsc(RouteStatus.ACTIVE, TripType.LOCAL)).thenReturn(List.of(route));
        when(routeStops.findByRouteIdAndActiveTrueOrderByStopOrderAsc(50L)).thenReturn(List.of(
                routeStop(route, stop1, 1, 0, "0"),
                routeStop(route, stop2, 2, 10, "20"),
                routeStop(route, stop3, 3, 25, "45")
        ));

        var result = service.searchByStopIds("passenger@test.local", 2L, 3L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).estimatedFare()).isEqualByComparingTo("25");
        assertThat(result.get(0).segmentDurationMinutes()).isEqualTo(15);
        assertThat(result.get(0).intermediateStopCount()).isZero();
    }

    @Test
    void reverseLocalRouteSearchDoesNotMatch() {
        PassengerLocalRouteService service = new PassengerLocalRouteService(users, routes, routeStops);
        User passenger = new User("Passenger", "passenger@test.local", "9800000000", "encoded", "PASSENGER");
        when(users.findByEmailIgnoreCase("passenger@test.local")).thenReturn(Optional.of(passenger));
        Route route = localRoute();
        when(routes.findByStatusAndTripTypeOrderByCodeAsc(RouteStatus.ACTIVE, TripType.LOCAL)).thenReturn(List.of(route));
        when(routeStops.findByRouteIdAndActiveTrueOrderByStopOrderAsc(50L)).thenReturn(List.of(
                routeStop(route, stop(1L, "Gongabu", true), 1, 0, "0"),
                routeStop(route, stop(2L, "Balaju", true), 2, 10, "20"),
                routeStop(route, stop(3L, "Ratnapark", true), 3, 25, "45")
        ));

        assertThat(service.searchByStopIds("passenger@test.local", 3L, 2L)).isEmpty();
    }

    @Test
    void inactiveStopsAreExcludedFromPassengerSearch() {
        PassengerLocalRouteService service = new PassengerLocalRouteService(users, routes, routeStops);
        User passenger = new User("Passenger", "passenger@test.local", "9800000000", "encoded", "PASSENGER");
        when(users.findByEmailIgnoreCase("passenger@test.local")).thenReturn(Optional.of(passenger));
        Route route = localRoute();
        when(routes.findByStatusAndTripTypeOrderByCodeAsc(RouteStatus.ACTIVE, TripType.LOCAL)).thenReturn(List.of(route));
        when(routeStops.findByRouteIdAndActiveTrueOrderByStopOrderAsc(50L)).thenReturn(List.of(
                routeStop(route, stop(1L, "Gongabu", true), 1, 0, "0"),
                routeStop(route, stop(2L, "Balaju", false), 2, 10, "20")
        ));

        assertThat(service.searchByStopIds("passenger@test.local", 1L, 2L)).isEmpty();
    }

    private Route localRoute() {
        Route route = new Route();
        route.setId(50L);
        route.setCode("LOCAL-50");
        route.setName("Local Demo Route");
        route.setOrigin("Gongabu");
        route.setDestination("Ratnapark");
        route.setDistanceKm(new BigDecimal("8.00"));
        route.setEstimatedDurationMinutes(30);
        route.setTripType(TripType.LOCAL);
        route.setStatus(RouteStatus.ACTIVE);
        return route;
    }

    private BusStop stop(Long id, String name, boolean active) {
        BusStop stop = new BusStop();
        stop.setId(id);
        stop.setName(name);
        stop.setActive(active);
        return stop;
    }

    private RouteStop routeStop(Route route, BusStop stop, int order, int minutes, String fare) {
        RouteStop routeStop = new RouteStop();
        routeStop.setRoute(route);
        routeStop.setBusStop(stop);
        routeStop.setStopOrder(order);
        routeStop.setEstimatedMinutesFromStart(minutes);
        routeStop.setCumulativeFare(new BigDecimal(fare));
        routeStop.setActive(true);
        return routeStop;
    }
}
