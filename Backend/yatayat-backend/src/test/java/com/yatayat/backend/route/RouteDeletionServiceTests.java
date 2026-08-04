package com.yatayat.backend.route;

import com.yatayat.backend.entity.Route;
import com.yatayat.backend.entity.RouteStatus;
import com.yatayat.backend.entity.TripType;
import com.yatayat.backend.repository.LocalFarePassRepository;
import com.yatayat.backend.repository.LocalServiceRunRepository;
import com.yatayat.backend.repository.RouteRepository;
import com.yatayat.backend.repository.RouteStopRepository;
import com.yatayat.backend.repository.ScheduledTripRepository;
import com.yatayat.backend.service.RouteDeletionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RouteDeletionServiceTests {

    @Mock RouteRepository routeRepository;
    @Mock RouteStopRepository routeStopRepository;
    @Mock ScheduledTripRepository scheduledTripRepository;
    @Mock LocalServiceRunRepository localServiceRunRepository;
    @Mock LocalFarePassRepository localFarePassRepository;

    private RouteDeletionService service;

    @BeforeEach
    void setUp() {
        service = new RouteDeletionService(routeRepository, routeStopRepository,
                scheduledTripRepository, localServiceRunRepository, localFarePassRepository);
    }

    @Test
    void deletesUnusedLocalRouteAndOnlyItsRouteStopLinks() {
        Route route = route(1L, TripType.LOCAL);
        when(routeRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(route));

        service.deleteUnusedRoute(1L);

        verify(routeStopRepository).deleteByRouteId(1L);
        verify(routeStopRepository).flush();
        verify(routeRepository).delete(route);
        verify(routeRepository).flush();
    }

    @Test
    void deletesUnusedOutOfValleyRoute() {
        Route route = route(2L, TripType.OUT_OF_VALLEY);
        when(routeRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(route));

        service.deleteUnusedRoute(2L);

        verify(routeRepository).delete(route);
    }

    @Test
    void unknownRouteReturnsNotFound() {
        when(routeRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteUnusedRoute(99L))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(exception.getReason()).isEqualTo("Route not found");
                });
    }

    @Test
    void scheduledTripReferenceBlocksDeletionWithoutRemovingDependents() {
        Route route = route(3L, TripType.OUT_OF_VALLEY);
        when(routeRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(route));
        when(scheduledTripRepository.existsByRoute(route)).thenReturn(true);

        assertReferencedConflict(route);
        verify(routeStopRepository, never()).deleteByRouteId(3L);
        verify(routeRepository, never()).delete(route);
    }

    @Test
    void localServiceReferenceBlocksDeletionWithoutRemovingDependents() {
        Route route = route(4L, TripType.LOCAL);
        when(routeRepository.findByIdForUpdate(4L)).thenReturn(Optional.of(route));
        when(localServiceRunRepository.existsByRoute(route)).thenReturn(true);

        assertReferencedConflict(route);
        verify(routeStopRepository, never()).deleteByRouteId(4L);
        verify(routeRepository, never()).delete(route);
    }

    @Test
    void historicalLocalFarePassBlocksDeletion() {
        Route route = route(5L, TripType.LOCAL);
        when(routeRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(route));
        when(localFarePassRepository.existsByRoute(route)).thenReturn(true);

        assertReferencedConflict(route);
        verify(routeRepository, never()).delete(route);
    }

    private void assertReferencedConflict(Route route) {
        assertThatThrownBy(() -> service.deleteUnusedRoute(route.getId()))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.getReason())
                            .contains("operational history")
                            .contains("Deactivate");
                });
    }

    private Route route(Long id, TripType type) {
        Route route = new Route();
        route.setId(id);
        route.setCode("ROUTE-" + id);
        route.setTripType(type);
        route.setStatus(RouteStatus.ACTIVE);
        return route;
    }
}
