package com.yatayat.backend.route;

import com.yatayat.backend.entity.Route;
import com.yatayat.backend.entity.RouteStatus;
import com.yatayat.backend.entity.TripType;
import com.yatayat.backend.repository.RouteRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class RouteTripTypePersistenceTests {

    @Autowired private RouteRepository routeRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void explicitLocalEnumIsStoredAsLocalRatherThanNull() {
        Route route = route("DB-LOCAL-01");
        route.setTripType(TripType.LOCAL);

        Route saved = routeRepository.saveAndFlush(route);

        String stored = jdbcTemplate.queryForObject(
                "select trip_type from routes where id = ?", String.class, saved.getId());
        assertThat(stored).isEqualTo("LOCAL");
        assertThat(saved.getTripType()).isEqualTo(TripType.LOCAL);
    }

    @Test
    void explicitOutsideValleyEnumIsStoredAsOutsideValley() {
        Route route = route("DB-OUT-01");
        route.setTripType(TripType.OUT_OF_VALLEY);

        Route saved = routeRepository.saveAndFlush(route);

        String stored = jdbcTemplate.queryForObject(
                "select trip_type from routes where id = ?", String.class, saved.getId());
        assertThat(stored).isEqualTo("OUT_OF_VALLEY");
    }

    @Test
    void legacyNullValueIsReadAsOutsideValley() {
        Route legacy = new Route();
        assertThat(legacy.getTripType()).isEqualTo(TripType.OUT_OF_VALLEY);
    }

    private Route route(String code) {
        Route route = new Route();
        route.setCode(code);
        route.setName("Persistence Route");
        route.setOrigin("Origin");
        route.setDestination("Destination");
        route.setDistanceKm(new BigDecimal("10.00"));
        route.setEstimatedDurationMinutes(30);
        route.setStatus(RouteStatus.ACTIVE);
        return route;
    }
}
