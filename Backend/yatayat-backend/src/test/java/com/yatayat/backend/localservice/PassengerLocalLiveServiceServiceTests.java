package com.yatayat.backend.localservice;

import com.yatayat.backend.dto.PassengerLocalLiveServiceResponse;
import com.yatayat.backend.entity.*;
import com.yatayat.backend.repository.LocalServiceLocationRepository;
import com.yatayat.backend.repository.LocalServiceRunRepository;
import com.yatayat.backend.service.PassengerLocalLiveServiceService;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PassengerLocalLiveServiceServiceTests {
    @Mock private LocalServiceRunRepository runRepository;
    @Mock private LocalServiceLocationRepository locationRepository;
    private PassengerLocalLiveServiceService service;
    private LocalServiceRun run;

    @BeforeEach
    void setUp() {
        service = new PassengerLocalLiveServiceService(runRepository, locationRepository);
        TransportOperator operator = new TransportOperator();
        operator.setId(1L);
        com.yatayat.backend.entity.Route route = new com.yatayat.backend.entity.Route();
        route.setId(2L);
        route.setCode("LOCAL-1");
        route.setName("Ring Road");
        route.setOrigin("Koteshwor");
        route.setDestination("Kalanki");
        Bus bus = new Bus();
        bus.setId(3L);
        bus.setBusNumber("BA-1-KHA-1234");
        bus.setBusName("City Bus");
        run = new LocalServiceRun();
        run.setId(4L);
        run.setOperator(operator);
        run.setRoute(route);
        run.setBus(bus);
        run.setStatus(LocalServiceRunStatus.IN_SERVICE);
    }

    @Test
    void activeServicesIncludeLatestLocation() {
        LocalServiceLocation location = location();
        when(runRepository.findByStatusOrderByServiceDateAscPlannedStartTimeAsc(LocalServiceRunStatus.IN_SERVICE))
                .thenReturn(List.of(run));
        when(locationRepository.findByRunIn(List.of(run))).thenReturn(List.of(location));

        List<PassengerLocalLiveServiceResponse> response = service.activeServices(null);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).runId()).isEqualTo(4L);
        assertThat(response.get(0).latitude()).isEqualTo(27.7);
        assertThat(response.get(0).busNumber()).isEqualTo("BA-1-KHA-1234");
    }

    @Test
    void missingGpsIsRepresentedWithNullCoordinates() {
        when(runRepository.findByStatusOrderByServiceDateAscPlannedStartTimeAsc(LocalServiceRunStatus.IN_SERVICE))
                .thenReturn(List.of(run));
        when(locationRepository.findByRunIn(List.of(run))).thenReturn(List.of());

        PassengerLocalLiveServiceResponse response = service.activeServices(null).get(0);

        assertThat(response.latitude()).isNull();
        assertThat(response.updatedAt()).isNull();
    }

    @Test
    void routeFilterUsesActiveRouteQuery() {
        when(runRepository.findByStatusAndRouteIdOrderByServiceDateAscPlannedStartTimeAsc(
                LocalServiceRunStatus.IN_SERVICE, 2L)).thenReturn(List.of(run));
        when(locationRepository.findByRunIn(List.of(run))).thenReturn(List.of());

        assertThat(service.activeServices(2L)).hasSize(1);
        verify(runRepository).findByStatusAndRouteIdOrderByServiceDateAscPlannedStartTimeAsc(
                LocalServiceRunStatus.IN_SERVICE, 2L);
    }

    @Test
    void singleLookupReturnsOnlyInServiceRun() {
        when(runRepository.findByIdAndStatus(4L, LocalServiceRunStatus.IN_SERVICE))
                .thenReturn(Optional.of(run));
        when(locationRepository.findByRun(run)).thenReturn(Optional.of(location()));

        assertThat(service.activeService(4L).serviceStatus()).isEqualTo(LocalServiceRunStatus.IN_SERVICE);
    }

    @Test
    void missingOrInactiveSingleRunReturnsNotFound() {
        when(runRepository.findByIdAndStatus(4L, LocalServiceRunStatus.IN_SERVICE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.activeService(4L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    private LocalServiceLocation location() {
        LocalServiceLocation location = new LocalServiceLocation();
        location.setRun(run);
        location.setLatitude(27.7);
        location.setLongitude(85.3);
        location.setUpdatedAt(LocalDateTime.now());
        return location;
    }
}
