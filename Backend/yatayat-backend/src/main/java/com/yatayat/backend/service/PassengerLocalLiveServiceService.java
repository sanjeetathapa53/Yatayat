package com.yatayat.backend.service;

import com.yatayat.backend.dto.PassengerLocalLiveServiceResponse;
import com.yatayat.backend.entity.LocalServiceLocation;
import com.yatayat.backend.entity.LocalServiceRun;
import com.yatayat.backend.entity.LocalServiceRunStatus;
import com.yatayat.backend.repository.LocalServiceLocationRepository;
import com.yatayat.backend.repository.LocalServiceRunRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PassengerLocalLiveServiceService {
    private final LocalServiceRunRepository runRepository;
    private final LocalServiceLocationRepository locationRepository;

    public PassengerLocalLiveServiceService(
            LocalServiceRunRepository runRepository,
            LocalServiceLocationRepository locationRepository
    ) {
        this.runRepository = runRepository;
        this.locationRepository = locationRepository;
    }

    @Transactional(readOnly = true)
    public List<PassengerLocalLiveServiceResponse> activeServices(Long routeId) {
        List<LocalServiceRun> runs = routeId == null
                ? runRepository.findByStatusOrderByServiceDateAscPlannedStartTimeAsc(
                        LocalServiceRunStatus.IN_SERVICE)
                : runRepository.findByStatusAndRouteIdOrderByServiceDateAscPlannedStartTimeAsc(
                        LocalServiceRunStatus.IN_SERVICE, routeId);
        if (runs.isEmpty()) return Collections.emptyList();

        Map<Long, LocalServiceLocation> locationsByRunId =
                locationRepository.findByRunIn(runs).stream()
                        .collect(Collectors.toMap(
                                location -> location.getRun().getId(),
                                Function.identity()
                        ));
        return runs.stream()
                .map(run -> toResponse(run, locationsByRunId.get(run.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public PassengerLocalLiveServiceResponse activeService(Long runId) {
        LocalServiceRun run = runRepository.findByIdAndStatus(
                        runId, LocalServiceRunStatus.IN_SERVICE)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Active local service not found."));
        return toResponse(run, locationRepository.findByRun(run).orElse(null));
    }

    private PassengerLocalLiveServiceResponse toResponse(
            LocalServiceRun run,
            LocalServiceLocation location
    ) {
        return new PassengerLocalLiveServiceResponse(
                run.getId(),
                run.getRoute().getId(),
                run.getRoute().getCode(),
                run.getRoute().getName(),
                run.getRoute().getOrigin(),
                run.getRoute().getDestination(),
                run.getBus().getId(),
                run.getBus().getBusNumber(),
                run.getBus().getBusName(),
                run.getServiceDate(),
                run.getPlannedStartTime(),
                run.getPlannedEndTime(),
                location == null ? null : location.getLatitude(),
                location == null ? null : location.getLongitude(),
                location == null ? null : location.getSpeed(),
                location == null ? null : location.getHeading(),
                location == null ? null : location.getUpdatedAt(),
                run.getStatus()
        );
    }
}
