package com.yatayat.backend.service;

import com.yatayat.backend.dto.BusStopRequest;
import com.yatayat.backend.dto.BusStopResponse;
import com.yatayat.backend.entity.BusStop;
import com.yatayat.backend.repository.BusStopRepository;
import jakarta.transaction.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;

@Service
public class BusStopService {
    private static final BigDecimal MIN_LATITUDE = new BigDecimal("-90");
    private static final BigDecimal MAX_LATITUDE = new BigDecimal("90");
    private static final BigDecimal MIN_LONGITUDE = new BigDecimal("-180");
    private static final BigDecimal MAX_LONGITUDE = new BigDecimal("180");

    private final BusStopRepository busStopRepository;

    public BusStopService(BusStopRepository busStopRepository) {
        this.busStopRepository = busStopRepository;
    }

    public List<BusStopResponse> getStops() {
        return busStopRepository.findAllByOrderByNameAsc().stream().map(this::toResponse).toList();
    }

    public BusStopResponse getStop(Long id) {
        return toResponse(findStop(id));
    }

    public List<BusStopResponse> searchActiveStops(String query) {
        String safeQuery = query == null ? "" : query.trim();
        if (safeQuery.isBlank()) {
            return List.of();
        }
        LinkedHashMap<Long, BusStopResponse> results = new LinkedHashMap<>();
        busStopRepository.findTop20ByActiveTrueAndNameContainingIgnoreCaseOrderByNameAsc(safeQuery)
                .forEach(stop -> results.put(stop.getId(), toResponse(stop)));
        if (results.size() < 20) {
            busStopRepository.findTop20ByActiveTrueAndLandmarkContainingIgnoreCaseOrderByNameAsc(safeQuery)
                    .forEach(stop -> results.putIfAbsent(stop.getId(), toResponse(stop)));
        }
        return results.values().stream().limit(20).toList();
    }

    @Transactional
    public BusStopResponse createStop(BusStopRequest request) {
        validate(request);
        if (busStopRepository.existsByNormalizedName(normalized(request.name()))) {
            conflict("Bus stop already exists");
        }
        BusStop stop = new BusStop();
        apply(stop, request);
        try {
            return toResponse(busStopRepository.saveAndFlush(stop));
        } catch (DataIntegrityViolationException exception) {
            conflict("Bus stop already exists");
            return null;
        }
    }

    @Transactional
    public BusStopResponse updateStop(Long id, BusStopRequest request) {
        validate(request);
        BusStop stop = findStop(id);
        if (busStopRepository.existsByNormalizedNameAndIdNot(normalized(request.name()), id)) {
            conflict("Bus stop already exists");
        }
        apply(stop, request);
        try {
            return toResponse(busStopRepository.saveAndFlush(stop));
        } catch (DataIntegrityViolationException exception) {
            conflict("Bus stop already exists");
            return null;
        }
    }

    @Transactional
    public BusStopResponse setActive(Long id, boolean active) {
        BusStop stop = findStop(id);
        stop.setActive(active);
        return toResponse(busStopRepository.saveAndFlush(stop));
    }

    private BusStop findStop(Long id) {
        return busStopRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bus stop not found"));
    }

    private void apply(BusStop stop, BusStopRequest request) {
        stop.setName(request.name());
        stop.setLatitude(request.latitude());
        stop.setLongitude(request.longitude());
        stop.setLandmark(request.landmark());
        stop.setActive(request.active() == null || request.active());
    }

    private void validate(BusStopRequest request) {
        if (request == null) badRequest("Bus stop details are required");
        if (request.name() == null || request.name().isBlank()) badRequest("Stop name is required");
        if (request.name().trim().length() > 160) badRequest("Stop name must not exceed 160 characters");
        if (request.landmark() != null && request.landmark().trim().length() > 255) {
            badRequest("Landmark must not exceed 255 characters");
        }
        validateRange(request.latitude(), MIN_LATITUDE, MAX_LATITUDE, "Latitude");
        validateRange(request.longitude(), MIN_LONGITUDE, MAX_LONGITUDE, "Longitude");
    }

    private void validateRange(BigDecimal value, BigDecimal min, BigDecimal max, String label) {
        if (value == null) return;
        if (value.compareTo(min) < 0 || value.compareTo(max) > 0) {
            badRequest(label + " is outside the valid range");
        }
    }

    private String normalized(String value) {
        return value == null ? null : value.trim().replaceAll("\\s+", " ").toUpperCase();
    }

    private BusStopResponse toResponse(BusStop stop) {
        return new BusStopResponse(stop.getId(), stop.getName(), stop.getLatitude(), stop.getLongitude(),
                stop.getLandmark(), stop.isActive(), stop.getCreatedAt(), stop.getUpdatedAt());
    }

    private void badRequest(String message) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private void conflict(String message) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, message);
    }
}
