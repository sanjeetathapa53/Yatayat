package com.yatayat.backend.service;

import com.yatayat.backend.dto.AdminBusResponse;
import com.yatayat.backend.entity.Bus;
import com.yatayat.backend.entity.BusStatus;
import com.yatayat.backend.entity.TransportOperator;
import com.yatayat.backend.repository.BusRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdminBusService {

    private final BusRepository busRepository;

    public AdminBusService(BusRepository busRepository) {
        this.busRepository = busRepository;
    }

    public List<AdminBusResponse> getBuses(String status) {
        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) {
            return busRepository.findAllByOrderByCreatedAtDesc()
                    .stream().map(this::toResponse).toList();
        }

        BusStatus requestedStatus;
        try {
            requestedStatus = BusStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid bus status filter");
        }

        return busRepository.findByStatusOrderByCreatedAtDesc(requestedStatus)
                .stream().map(this::toResponse).toList();
    }

    public AdminBusResponse getBus(Long busId) {
        return toResponse(findBus(busId));
    }

    @Transactional
    public AdminBusResponse approve(Long busId) {
        Bus bus = findPendingBus(busId);
        bus.setStatus(BusStatus.APPROVED);
        bus.setApprovedAt(LocalDateTime.now());
        bus.setRejectionReason(null);
        return toResponse(busRepository.saveAndFlush(bus));
    }

    @Transactional
    public AdminBusResponse reject(Long busId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rejection reason is required");
        }
        if (reason.trim().length() > 1000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rejection reason must not exceed 1000 characters");
        }

        Bus bus = findPendingBus(busId);
        bus.setStatus(BusStatus.REJECTED);
        bus.setApprovedAt(null);
        bus.setRejectionReason(reason);
        return toResponse(busRepository.saveAndFlush(bus));
    }

    private Bus findBus(Long busId) {
        return busRepository.findById(busId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Bus not found"));
    }

    private Bus findPendingBus(Long busId) {
        Bus bus = findBus(busId);
        if (bus.getStatus() != BusStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only pending buses can be reviewed"
            );
        }
        return bus;
    }

    private AdminBusResponse toResponse(Bus bus) {
        TransportOperator operator = bus.getOperator();
        return new AdminBusResponse(
                bus.getId(), bus.getBusNumber(), bus.getBusName(),
                bus.getModel(), bus.getManufactureYear(), bus.getSeatCapacity(),
                bus.getBusType(), bus.getFuelType(), bus.getPermitNumber(),
                bus.getPermitExpiryDate(), bus.getInsuranceExpiryDate(),
                bus.getStatus().name(), bus.getRejectionReason(), bus.getApprovedAt(),
                bus.getCreatedAt(), bus.getUpdatedAt(), operator.getId(),
                operator.getName(), operator.getEmail(), operator.getPhone(),
                operator.getRegistrationNumber()
        );
    }
}
