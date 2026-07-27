package com.yatayat.backend.localservice;

import com.yatayat.backend.dto.OperatorLocalFleetLocationResponse;
import com.yatayat.backend.entity.*;
import com.yatayat.backend.repository.LocalServiceLocationRepository;
import com.yatayat.backend.repository.LocalServiceRunRepository;
import com.yatayat.backend.repository.TransportOperatorRepository;
import com.yatayat.backend.repository.UserRepository;
import com.yatayat.backend.service.OperatorLocalLiveFleetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperatorLocalLiveFleetServiceTests {
    @Mock private UserRepository userRepository;
    @Mock private TransportOperatorRepository operatorRepository;
    @Mock private LocalServiceRunRepository runRepository;
    @Mock private LocalServiceLocationRepository locationRepository;

    private OperatorLocalLiveFleetService service;
    private User user;
    private TransportOperator operator;
    private LocalServiceRun run;

    @BeforeEach
    void setUp() {
        service = new OperatorLocalLiveFleetService(
                userRepository, operatorRepository, runRepository, locationRepository);
        user = new User("Operator", "operator@example.com", "9800000000", "encoded", "OPERATOR");
        operator = new TransportOperator();
        operator.setId(2L);
        operator.setUser(user);
        operator.setVerificationStatus(OperatorVerificationStatus.APPROVED);
        when(userRepository.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
        when(operatorRepository.findByUser(user)).thenReturn(Optional.of(operator));
        run = localRun();
    }

    @Test
    void activeFleetIsScopedToOperatorAndInServiceStatus() {
        LocalServiceLocation location = location();
        when(runRepository.findByOperatorAndStatusOrderByServiceDateAscPlannedStartTimeAsc(
                operator, LocalServiceRunStatus.IN_SERVICE)).thenReturn(List.of(run));
        when(locationRepository.findByRunIn(List.of(run))).thenReturn(List.of(location));

        OperatorLocalFleetLocationResponse response =
                service.activeFleet(user.getEmail()).get(0);

        assertThat(response.runId()).isEqualTo(10L);
        assertThat(response.latitude()).isEqualTo(27.7);
        assertThat(response.serviceStatus()).isEqualTo(LocalServiceRunStatus.IN_SERVICE);
        verify(runRepository).findByOperatorAndStatusOrderByServiceDateAscPlannedStartTimeAsc(
                operator, LocalServiceRunStatus.IN_SERVICE);
    }

    @Test
    void activeRunWithoutGpsHasNullCoordinates() {
        when(runRepository.findByOperatorAndStatusOrderByServiceDateAscPlannedStartTimeAsc(
                operator, LocalServiceRunStatus.IN_SERVICE)).thenReturn(List.of(run));
        when(locationRepository.findByRunIn(List.of(run))).thenReturn(List.of());

        OperatorLocalFleetLocationResponse response =
                service.activeFleet(user.getEmail()).get(0);

        assertThat(response.latitude()).isNull();
        assertThat(response.updatedAt()).isNull();
    }

    @Test
    void singleLookupRequiresSameOperatorAndInServiceStatus() {
        when(runRepository.findByIdAndOperatorAndStatus(
                10L, operator, LocalServiceRunStatus.IN_SERVICE)).thenReturn(Optional.of(run));
        when(locationRepository.findByRun(run)).thenReturn(Optional.of(location()));

        assertThat(service.run(user.getEmail(), 10L).runId()).isEqualTo(10L);
    }

    @Test
    void anotherOperatorsOrInactiveRunIsNotFound() {
        when(runRepository.findByIdAndOperatorAndStatus(
                99L, operator, LocalServiceRunStatus.IN_SERVICE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.run(user.getEmail(), 99L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    private LocalServiceRun localRun() {
        Bus bus = new Bus();
        bus.setId(3L);
        bus.setBusNumber("BA 1 PA 1234");
        bus.setBusName("City Bus");
        User driverUser =
                new User("Driver One", "driver@example.com", "9800000001", "encoded", "DRIVER");
        DriverProfile driver = new DriverProfile(driverUser);
        driver.setId(4L);
        com.yatayat.backend.entity.Route route = new com.yatayat.backend.entity.Route();
        route.setId(5L);
        route.setName("Ring Road");
        route.setOrigin("Kalanki");
        route.setDestination("Koteshwor");
        LocalServiceRun localRun = new LocalServiceRun();
        localRun.setId(10L);
        localRun.setOperator(operator);
        localRun.setBus(bus);
        localRun.setDriver(driver);
        localRun.setRoute(route);
        localRun.setStatus(LocalServiceRunStatus.IN_SERVICE);
        return localRun;
    }

    private LocalServiceLocation location() {
        LocalServiceLocation location = new LocalServiceLocation();
        location.setRun(run);
        location.setLatitude(27.7);
        location.setLongitude(85.3);
        location.setSpeed(8.0);
        location.setHeading(45.0);
        location.setUpdatedAt(LocalDateTime.of(2026, 7, 27, 12, 0));
        return location;
    }
}
