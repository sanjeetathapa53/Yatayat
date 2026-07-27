package com.yatayat.backend.service;

import com.yatayat.backend.dto.AdminLiveMonitoringResponse;
import com.yatayat.backend.entity.*;
import com.yatayat.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AdminLiveMonitoringService {
    private final ScheduledTripRepository tripRepository;
    private final TripLocationRepository tripLocationRepository;
    private final LocalServiceRunRepository runRepository;
    private final LocalServiceLocationRepository localLocationRepository;
    private final Duration liveThreshold;
    private final Duration offlineThreshold;
    private final Duration upcomingWindow;
    private final Clock clock;

    @Autowired
    public AdminLiveMonitoringService(
            ScheduledTripRepository tripRepository,
            TripLocationRepository tripLocationRepository,
            LocalServiceRunRepository runRepository,
            LocalServiceLocationRepository localLocationRepository,
            @Value("${yatayat.monitoring.live-threshold-seconds:30}") long liveSeconds,
            @Value("${yatayat.monitoring.offline-threshold-seconds:300}") long offlineSeconds,
            @Value("${yatayat.monitoring.upcoming-trip-window-minutes:120}") long upcomingMinutes) {
        this(tripRepository, tripLocationRepository, runRepository, localLocationRepository,
                liveSeconds, offlineSeconds, upcomingMinutes, Clock.systemDefaultZone());
    }

    public AdminLiveMonitoringService(
            ScheduledTripRepository tripRepository,
            TripLocationRepository tripLocationRepository,
            LocalServiceRunRepository runRepository,
            LocalServiceLocationRepository localLocationRepository,
            long liveSeconds, long offlineSeconds, long upcomingMinutes, Clock clock) {
        if (liveSeconds < 1 || offlineSeconds <= liveSeconds || upcomingMinutes < 0) {
            throw new IllegalArgumentException("Live monitoring thresholds are invalid.");
        }
        this.tripRepository = tripRepository;
        this.tripLocationRepository = tripLocationRepository;
        this.runRepository = runRepository;
        this.localLocationRepository = localLocationRepository;
        this.liveThreshold = Duration.ofSeconds(liveSeconds);
        this.offlineThreshold = Duration.ofSeconds(offlineSeconds);
        this.upcomingWindow = Duration.ofMinutes(upcomingMinutes);
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AdminLiveMonitoringResponse snapshot() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<ScheduledTrip> trips = tripRepository.findAdminMonitoredTrips(
                now, now.plus(upcomingWindow),
                List.of(TripStatus.BOARDING, TripStatus.IN_PROGRESS));
        List<LocalServiceRun> runs = runRepository.findAdminMonitoredRuns(LocalServiceRunStatus.IN_SERVICE);

        Map<Long, TripLocation> tripLocations = trips.isEmpty() ? Map.of()
                : tripLocationRepository.findByTripIn(trips).stream()
                .collect(Collectors.toMap(value -> value.getTrip().getId(), Function.identity()));
        Map<Long, LocalServiceLocation> localLocations = runs.isEmpty() ? Map.of()
                : localLocationRepository.findByRunIn(runs).stream()
                .collect(Collectors.toMap(value -> value.getRun().getId(), Function.identity()));

        List<AdminLiveMonitoringResponse.Vehicle> vehicles = new ArrayList<>();
        trips.stream().map(trip -> scheduled(trip, tripLocations.get(trip.getId()), now))
                .forEach(vehicles::add);
        runs.stream().map(run -> local(run, localLocations.get(run.getId()), now))
                .forEach(vehicles::add);
        return new AdminLiveMonitoringResponse(now, List.copyOf(vehicles));
    }

    private AdminLiveMonitoringResponse.Vehicle scheduled(
            ScheduledTrip trip, TripLocation location, LocalDateTime now) {
        return vehicle(trip.getBus(), trip.getOperator(), trip.getDriver(), trip.getRoute(),
                trip.getId(), "SCHEDULED_TRIP", "OUT_OF_VALLEY", trip.getStatus().name(),
                location == null ? null : location.getLatitude(),
                location == null ? null : location.getLongitude(),
                location == null ? null : location.getSpeed(),
                location == null ? null : location.getHeading(),
                location == null ? null : location.getUpdatedAt(), now);
    }

    private AdminLiveMonitoringResponse.Vehicle local(
            LocalServiceRun run, LocalServiceLocation location, LocalDateTime now) {
        return vehicle(run.getBus(), run.getOperator(), run.getDriver(), run.getRoute(),
                run.getId(), "LOCAL_SERVICE", "LOCAL", run.getStatus().name(),
                location == null ? null : location.getLatitude(),
                location == null ? null : location.getLongitude(),
                location == null ? null : location.getSpeed(),
                location == null ? null : location.getHeading(),
                location == null ? null : location.getUpdatedAt(), now);
    }

    private AdminLiveMonitoringResponse.Vehicle vehicle(
            Bus bus, TransportOperator operator, DriverProfile driver, Route route,
            Long operationId, String operationType, String tripType, String operationStatus,
            Double latitude, Double longitude, Double speed, Double heading,
            LocalDateTime updatedAt, LocalDateTime now) {
        Long age = updatedAt == null ? null : Math.max(0, Duration.between(updatedAt, now).getSeconds());
        return new AdminLiveMonitoringResponse.Vehicle(
                bus.getId(), bus.getBusNumber(), bus.getBusName(), bus.getStatus().name(),
                operator.getId(), operator.getName(),
                driver.getId(), driver.getUser().getFullName(), driverStatus(driver),
                route.getId(), route.getName(), route.getOrigin(), route.getDestination(),
                tripType, operationId, operationType, operationStatus,
                latitude, longitude, speed, heading, updatedAt, age,
                freshness(latitude, longitude, updatedAt, now));
    }

    private String driverStatus(DriverProfile driver) {
        if (!driver.isApproved()) return "NOT_APPROVED";
        if (driver.isLicenseExpired()) return "LICENCE_EXPIRED";
        return "OPERATIONAL";
    }

    private String freshness(Double latitude, Double longitude, LocalDateTime updatedAt, LocalDateTime now) {
        if (!validCoordinates(latitude, longitude) || updatedAt == null) return "OFFLINE";
        Duration age = Duration.between(updatedAt, now);
        if (age.isNegative() || age.compareTo(liveThreshold) <= 0) return "LIVE";
        if (age.compareTo(offlineThreshold) <= 0) return "STALE";
        return "OFFLINE";
    }

    private boolean validCoordinates(Double latitude, Double longitude) {
        return latitude != null && longitude != null
                && Double.isFinite(latitude) && Double.isFinite(longitude)
                && latitude >= -90 && latitude <= 90
                && longitude >= -180 && longitude <= 180;
    }
}
