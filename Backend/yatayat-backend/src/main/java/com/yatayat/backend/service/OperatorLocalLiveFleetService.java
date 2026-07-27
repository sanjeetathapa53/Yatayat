package com.yatayat.backend.service;

import com.yatayat.backend.dto.OperatorLocalFleetLocationResponse;
import com.yatayat.backend.entity.*;
import com.yatayat.backend.repository.LocalServiceLocationRepository;
import com.yatayat.backend.repository.LocalServiceRunRepository;
import com.yatayat.backend.repository.TransportOperatorRepository;
import com.yatayat.backend.repository.UserRepository;
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
public class OperatorLocalLiveFleetService {
    private final UserRepository userRepository;
    private final TransportOperatorRepository operatorRepository;
    private final LocalServiceRunRepository runRepository;
    private final LocalServiceLocationRepository locationRepository;

    public OperatorLocalLiveFleetService(
            UserRepository userRepository,
            TransportOperatorRepository operatorRepository,
            LocalServiceRunRepository runRepository,
            LocalServiceLocationRepository locationRepository
    ) {
        this.userRepository = userRepository;
        this.operatorRepository = operatorRepository;
        this.runRepository = runRepository;
        this.locationRepository = locationRepository;
    }

    @Transactional(readOnly = true)
    public List<OperatorLocalFleetLocationResponse> activeFleet(String email) {
        TransportOperator operator = requireApprovedOperator(email);
        List<LocalServiceRun> runs =
                runRepository.findByOperatorAndStatusOrderByServiceDateAscPlannedStartTimeAsc(
                        operator, LocalServiceRunStatus.IN_SERVICE);
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
    public OperatorLocalFleetLocationResponse run(String email, Long runId) {
        TransportOperator operator = requireApprovedOperator(email);
        LocalServiceRun run = runRepository.findByIdAndOperatorAndStatus(
                        runId, operator, LocalServiceRunStatus.IN_SERVICE)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Active local service not found."));
        return toResponse(run, locationRepository.findByRun(run).orElse(null));
    }

    private TransportOperator requireApprovedOperator(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Authenticated operator not found."));
        if (!"OPERATOR".equalsIgnoreCase(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Operator access is required.");
        }
        TransportOperator operator = operatorRepository.findByUser(user)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Operator application not found."));
        if (!operator.isApproved()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Approved operator access is required.");
        }
        return operator;
    }

    private OperatorLocalFleetLocationResponse toResponse(
            LocalServiceRun run,
            LocalServiceLocation location
    ) {
        return new OperatorLocalFleetLocationResponse(
                run.getId(),
                run.getBus().getId(),
                run.getBus().getBusNumber(),
                run.getBus().getBusName(),
                run.getDriver().getId(),
                run.getDriver().getUser().getFullName(),
                run.getRoute().getId(),
                run.getRoute().getName(),
                run.getRoute().getOrigin(),
                run.getRoute().getDestination(),
                location == null ? null : location.getLatitude(),
                location == null ? null : location.getLongitude(),
                location == null ? null : location.getSpeed(),
                location == null ? null : location.getHeading(),
                location == null ? null : location.getUpdatedAt(),
                run.getStatus()
        );
    }
}
