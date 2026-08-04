package com.yatayat.backend.service;

import com.yatayat.backend.entity.Route;
import com.yatayat.backend.repository.LocalFarePassRepository;
import com.yatayat.backend.repository.LocalServiceRunRepository;
import com.yatayat.backend.repository.RouteRepository;
import com.yatayat.backend.repository.RouteStopRepository;
import com.yatayat.backend.repository.ScheduledTripRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RouteDeletionService {

    private static final String REFERENCED_ROUTE_MESSAGE =
            "Route is referenced by operational history and cannot be deleted. " +
                    "Deactivate the route instead.";

    private final RouteRepository routeRepository;
    private final RouteStopRepository routeStopRepository;
    private final ScheduledTripRepository scheduledTripRepository;
    private final LocalServiceRunRepository localServiceRunRepository;
    private final LocalFarePassRepository localFarePassRepository;

    public RouteDeletionService(
            RouteRepository routeRepository,
            RouteStopRepository routeStopRepository,
            ScheduledTripRepository scheduledTripRepository,
            LocalServiceRunRepository localServiceRunRepository,
            LocalFarePassRepository localFarePassRepository
    ) {
        this.routeRepository = routeRepository;
        this.routeStopRepository = routeStopRepository;
        this.scheduledTripRepository = scheduledTripRepository;
        this.localServiceRunRepository = localServiceRunRepository;
        this.localFarePassRepository = localFarePassRepository;
    }

    @Transactional
    public void deleteUnusedRoute(Long routeId) {
        Route route = routeRepository.findByIdForUpdate(routeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Route not found"));

        if (scheduledTripRepository.existsByRoute(route) ||
                localServiceRunRepository.existsByRoute(route) ||
                localFarePassRepository.existsByRoute(route)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, REFERENCED_ROUTE_MESSAGE);
        }

        try {
            routeStopRepository.deleteByRouteId(route.getId());
            routeStopRepository.flush();
            routeRepository.delete(route);
            routeRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, REFERENCED_ROUTE_MESSAGE);
        }
    }
}
