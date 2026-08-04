package com.yatayat.backend.route;

import com.yatayat.backend.entity.BusStop;
import com.yatayat.backend.entity.Route;
import com.yatayat.backend.entity.RouteStatus;
import com.yatayat.backend.entity.RouteStop;
import com.yatayat.backend.entity.TripType;
import com.yatayat.backend.repository.BusStopRepository;
import com.yatayat.backend.repository.LocalFarePassRepository;
import com.yatayat.backend.repository.LocalServiceRunRepository;
import com.yatayat.backend.repository.RouteRepository;
import com.yatayat.backend.repository.RouteStopRepository;
import com.yatayat.backend.repository.ScheduledTripRepository;
import com.yatayat.backend.service.RouteDeletionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class RouteDeletionPersistenceTests {

    @Autowired RouteRepository routeRepository;
    @Autowired RouteStopRepository routeStopRepository;
    @Autowired BusStopRepository busStopRepository;
    @Autowired ScheduledTripRepository scheduledTripRepository;
    @Autowired LocalServiceRunRepository localServiceRunRepository;
    @Autowired LocalFarePassRepository localFarePassRepository;

    private RouteDeletionService service;

    @BeforeEach
    void setUp() {
        service = new RouteDeletionService(routeRepository, routeStopRepository,
                scheduledTripRepository, localServiceRunRepository, localFarePassRepository);
    }

    @Test
    void deletingUnusedLocalRouteRemovesLinksButPreservesSharedStop() {
        BusStop sharedStop = new BusStop();
        sharedStop.setName("Shared Test Stop");
        sharedStop.setActive(true);
        sharedStop = busStopRepository.saveAndFlush(sharedStop);

        Route route = route("DELETE-LOCAL", TripType.LOCAL);
        route = routeRepository.saveAndFlush(route);

        RouteStop link = new RouteStop();
        link.setRoute(route);
        link.setBusStop(sharedStop);
        link.setStopOrder(1);
        link.setEstimatedMinutesFromStart(0);
        link.setCumulativeFare(BigDecimal.ZERO);
        link.setActive(true);
        routeStopRepository.saveAndFlush(link);

        service.deleteUnusedRoute(route.getId());

        assertThat(routeRepository.findById(route.getId())).isEmpty();
        assertThat(routeStopRepository.findByRouteIdOrderByStopOrderAsc(route.getId())).isEmpty();
        assertThat(busStopRepository.findById(sharedStop.getId())).isPresent();
    }

    @Test
    void deletingUnusedOutOfValleyRouteRemovesOnlyRoute() {
        Route route = routeRepository.saveAndFlush(
                route("DELETE-OUTSIDE", TripType.OUT_OF_VALLEY));

        service.deleteUnusedRoute(route.getId());

        assertThat(routeRepository.findById(route.getId())).isEmpty();
    }

    private Route route(String code, TripType type) {
        Route route = new Route();
        route.setCode(code);
        route.setName("Disposable route");
        route.setOrigin("Origin " + code);
        route.setDestination("Destination " + code);
        route.setDistanceKm(BigDecimal.TEN);
        route.setEstimatedDurationMinutes(30);
        route.setTripType(type);
        route.setStatus(RouteStatus.ACTIVE);
        return route;
    }
}
