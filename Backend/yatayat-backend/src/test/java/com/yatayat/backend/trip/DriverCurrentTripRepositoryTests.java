package com.yatayat.backend.trip;

import com.yatayat.backend.entity.*;
import com.yatayat.backend.repository.ScheduledTripRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class DriverCurrentTripRepositoryTests {
    private static final List<TripStatus> CURRENT_STATUSES =
            List.of(TripStatus.SCHEDULED, TripStatus.BOARDING, TripStatus.IN_PROGRESS);

    @Autowired EntityManager entityManager;
    @Autowired ScheduledTripRepository tripRepository;

    private DriverProfile driverA;
    private DriverProfile driverB;
    private TransportOperator operator;
    private Route route;
    private Bus bus;

    @BeforeEach
    void setUp() {
        operator = persistOperator();
        driverA = persistDriver("a");
        driverB = persistDriver("b");
        route = persistRoute();
        bus = persistBus();
    }

    @Test
    void overdueScheduledTripDoesNotHideNearestFutureAssignment() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 4, 12, 0);
        persistTrip(driverA, TripStatus.SCHEDULED, now.minusDays(2));
        ScheduledTrip future = persistTrip(driverA, TripStatus.SCHEDULED, now.plusHours(3));

        List<ScheduledTrip> current = tripRepository.findDriverOperationalTrips(
                driverA, CURRENT_STATUSES, now);

        assertThat(current).extracting(ScheduledTrip::getId)
                .containsExactly(future.getId());
    }

    @Test
    void prioritizesActiveStatusesAndKeepsAssignmentsDriverScoped() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 4, 12, 0);
        ScheduledTrip scheduled = persistTrip(driverA, TripStatus.SCHEDULED, now.plusHours(2));
        ScheduledTrip boarding = persistTrip(driverA, TripStatus.BOARDING, now.minusMinutes(15));
        ScheduledTrip active = persistTrip(driverA, TripStatus.IN_PROGRESS, now.minusHours(1));
        persistTrip(driverA, TripStatus.COMPLETED, now.minusDays(1));
        persistTrip(driverA, TripStatus.CANCELLED, now.plusHours(1));
        persistTrip(driverB, TripStatus.IN_PROGRESS, now.minusMinutes(30));

        List<ScheduledTrip> current = tripRepository.findDriverOperationalTrips(
                driverA, CURRENT_STATUSES, now);

        assertThat(current).extracting(ScheduledTrip::getId)
                .containsExactly(active.getId(), boarding.getId(), scheduled.getId());
    }

    private User persistUser(String name, String email, String role) {
        User user = new User(name, email, "9800000000", "encoded", role);
        entityManager.persist(user);
        return user;
    }

    private TransportOperator persistOperator() {
        TransportOperator value = new TransportOperator();
        value.setUser(persistUser("Operator", "operator-current@example.com", "OPERATOR"));
        value.setName("Current Trip Operator");
        value.setOperatorType(OperatorType.PRIVATE_COMPANY);
        value.setRegistrationNumber("CURRENT-OP-1");
        value.setContactPerson("Contact");
        value.setEmail("operator-current@example.com");
        value.setPhone("9800000001");
        value.setAddress("Kathmandu");
        value.setVerificationStatus(OperatorVerificationStatus.APPROVED);
        entityManager.persist(value);
        return value;
    }

    private DriverProfile persistDriver(String suffix) {
        DriverProfile value = new DriverProfile(
                persistUser("Driver " + suffix, "driver-" + suffix + "@example.com", "DRIVER"));
        value.setCitizenshipNumber("CIT-" + suffix);
        value.setLicenseNumber("LIC-" + suffix);
        value.setLicenseCategory("B");
        value.setLicenseExpiryDate(LocalDate.of(2030, 1, 1));
        value.setVerificationStatus(DriverVerificationStatus.APPROVED);
        entityManager.persist(value);
        return value;
    }

    private Route persistRoute() {
        Route value = new Route();
        value.setCode("CURRENT-ROUTE");
        value.setName("Kathmandu to Pokhara");
        value.setOrigin("Kathmandu");
        value.setDestination("Pokhara");
        value.setDistanceKm(new BigDecimal("200"));
        value.setEstimatedDurationMinutes(360);
        value.setTripType(TripType.OUT_OF_VALLEY);
        value.setStatus(RouteStatus.ACTIVE);
        entityManager.persist(value);
        return value;
    }

    private Bus persistBus() {
        Bus value = new Bus();
        value.setBusNumber("CURRENT-BUS-1");
        value.setBusName("Current Bus");
        value.setSeatCapacity(40);
        value.setBusType("DELUXE");
        value.setStatus(BusStatus.APPROVED);
        value.setOperator(operator);
        value.setOperatorName(operator.getName());
        entityManager.persist(value);
        return value;
    }

    private ScheduledTrip persistTrip(
            DriverProfile driver, TripStatus status, LocalDateTime departure) {
        ScheduledTrip value = new ScheduledTrip();
        value.setOperator(operator);
        value.setRoute(route);
        value.setBus(bus);
        value.setDriver(driver);
        value.setDepartureAt(departure);
        value.setEstimatedArrivalAt(departure.plusHours(6));
        value.setFare(new BigDecimal("1200"));
        value.setSeatCapacitySnapshot(40);
        value.setStatus(status);
        entityManager.persist(value);
        entityManager.flush();
        return value;
    }
}
