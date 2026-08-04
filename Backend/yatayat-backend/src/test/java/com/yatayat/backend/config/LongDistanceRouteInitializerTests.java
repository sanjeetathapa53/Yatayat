package com.yatayat.backend.config;

import com.yatayat.backend.entity.Route;
import com.yatayat.backend.entity.RouteStatus;
import com.yatayat.backend.entity.TripType;
import com.yatayat.backend.repository.BusStopRepository;
import com.yatayat.backend.repository.RouteRepository;
import com.yatayat.backend.repository.RouteStopRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class LongDistanceRouteInitializerTests {

    @Autowired RouteRepository routeRepository;
    @Autowired BusStopRepository busStopRepository;
    @Autowired RouteStopRepository routeStopRepository;

    private LongDistanceRouteInitializer initializer;

    @BeforeEach
    void setUp() {
        initializer = new LongDistanceRouteInitializer(
                routeRepository, busStopRepository, routeStopRepository);
    }

    @Test
    void seedsActiveOrderedOutOfValleyRoutesForOperatorEligibility() throws Exception {
        initializer.run();

        List<Route> routes = routeRepository.findByStatusAndTripTypeOrderByCodeAsc(
                RouteStatus.ACTIVE, TripType.OUT_OF_VALLEY);
        assertThat(routes).hasSize(LongDistanceRouteInitializer.catalogSize());
        assertThat(routes).allSatisfy(route -> {
            assertThat(route.getStatus()).isEqualTo(RouteStatus.ACTIVE);
            assertThat(route.getTripType()).isEqualTo(TripType.OUT_OF_VALLEY);
            assertThat(route.getDistanceKm()).isPositive();
            assertThat(route.getEstimatedDurationMinutes()).isPositive();

            var stops = routeStopRepository.findByRouteIdOrderByStopOrderAsc(route.getId());
            assertThat(stops).hasSizeGreaterThanOrEqualTo(3);
            assertThat(stops).extracting(stop -> stop.getStopOrder())
                    .containsExactlyElementsOf(IntStream.rangeClosed(1, stops.size()).boxed().toList());
            assertThat(stops.get(0).getBusStop().getName()).isEqualTo(route.getOrigin());
            assertThat(stops.get(stops.size() - 1).getBusStop().getName())
                    .isEqualTo(route.getDestination());
        });

        List<Route> scheduledTripSelectorRoutes = routeRepository
                .findByStatusOrderByCodeAsc(RouteStatus.ACTIVE).stream()
                .filter(route -> route.getTripType() == TripType.OUT_OF_VALLEY)
                .toList();
        assertThat(scheduledTripSelectorRoutes)
                .hasSize(LongDistanceRouteInitializer.catalogSize());
    }

    @Test
    void restartIsIdempotentAndPreservesExistingLocalAndMatchingRoutes() throws Exception {
        routeRepository.saveAndFlush(route(
                "LOCAL-EXISTING", "Kathmandu", "Bhaktapur", TripType.LOCAL));
        routeRepository.saveAndFlush(route(
                "CUSTOM-KTM-PKR", "Kathmandu", "Pokhara", TripType.OUT_OF_VALLEY));

        initializer.run();
        long routeCountAfterFirstRun = routeRepository.count();
        long stopCountAfterFirstRun = routeStopRepository.count();
        initializer.run();

        assertThat(routeRepository.count()).isEqualTo(routeCountAfterFirstRun);
        assertThat(routeStopRepository.count()).isEqualTo(stopCountAfterFirstRun);
        assertThat(routeRepository.findByTripTypeOrderByCodeAsc(TripType.LOCAL))
                .extracting(Route::getCode).containsExactly("LOCAL-EXISTING");
        assertThat(routeRepository.existsByCodeIgnoreCase("KTM-PKR")).isFalse();
        assertThat(routeRepository.existsByCodeIgnoreCase("CUSTOM-KTM-PKR")).isTrue();
    }

    private Route route(String code, String origin, String destination, TripType tripType) {
        Route route = new Route();
        route.setCode(code);
        route.setName(origin + " to " + destination);
        route.setOrigin(origin);
        route.setDestination(destination);
        route.setDistanceKm(BigDecimal.TEN);
        route.setEstimatedDurationMinutes(30);
        route.setTripType(tripType);
        route.setStatus(RouteStatus.ACTIVE);
        return route;
    }
}
