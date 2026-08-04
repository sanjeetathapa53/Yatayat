package com.yatayat.backend.route;

import com.yatayat.backend.entity.Route;
import com.yatayat.backend.repository.*;
import com.yatayat.backend.service.RouteDeletionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RouteDeletionServiceTests {
    @Mock RouteRepository routeRepository;
    @Mock RouteStopRepository routeStopRepository;
    @Mock ScheduledTripRepository scheduledTripRepository;
    @Mock LocalServiceRunRepository localServiceRunRepository;
    @Mock LocalFarePassRepository localFarePassRepository;
    RouteDeletionService service;
    Route route;

    @BeforeEach
    void setUp() {
        service = new RouteDeletionService(routeRepository, routeStopRepository,
                scheduledTripRepository, localServiceRunRepository, localFarePassRepository);
        route = new Route();
        route.setId(7L);
    }

    @Test
    void unusedRouteDeletesLinksThenRoute() {
        when(routeRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(route));
        service.deleteRoute(7L);
        var order = inOrder(routeStopRepository, routeRepository);
        order.verify(routeStopRepository).deleteByRouteId(7L);
        order.verify(routeStopRepository).flush();
        order.verify(routeRepository).delete(route);
        order.verify(routeRepository).flush();
    }

    @Test
    void unknownRouteReturnsNotFound() {
        when(routeRepository.findByIdForUpdate(7L)).thenReturn(Optional.empty());
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.deleteRoute(7L));
        assertEquals(404, error.getStatusCode().value());
    }

    @Test
    void scheduledTripReferenceReturnsConflict() {
        when(routeRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(route));
        when(scheduledTripRepository.existsByRoute(route)).thenReturn(true);
        assertConflict();
    }

    @Test
    void localServiceReferenceReturnsConflict() {
        when(routeRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(route));
        when(localServiceRunRepository.existsByRoute(route)).thenReturn(true);
        assertConflict();
    }

    @Test
    void localFarePassReferenceReturnsConflict() {
        when(routeRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(route));
        when(localFarePassRepository.existsByRoute(route)).thenReturn(true);
        assertConflict();
    }

    private void assertConflict() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.deleteRoute(7L));
        assertEquals(409, error.getStatusCode().value());
        assertEquals(RouteDeletionService.REFERENCED_MESSAGE, error.getReason());
        verify(routeRepository, never()).delete(any());
        verify(routeStopRepository, never()).deleteByRouteId(anyLong());
    }
}
