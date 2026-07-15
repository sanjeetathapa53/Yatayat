package com.yatayat.backend.service;

import com.yatayat.backend.dto.OperatorBusRequest;
import com.yatayat.backend.dto.OperatorBusResponse;
import com.yatayat.backend.entity.Bus;
import com.yatayat.backend.entity.BusStatus;
import com.yatayat.backend.entity.OperatorVerificationStatus;
import com.yatayat.backend.entity.TransportOperator;
import com.yatayat.backend.entity.User;
import com.yatayat.backend.repository.BusRepository;
import com.yatayat.backend.repository.TransportOperatorRepository;
import com.yatayat.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Year;
import java.util.List;

@Service
public class OperatorBusService {

    private final UserRepository userRepository;
    private final TransportOperatorRepository operatorRepository;
    private final BusRepository busRepository;

    public OperatorBusService(
            UserRepository userRepository,
            TransportOperatorRepository operatorRepository,
            BusRepository busRepository
    ) {
        this.userRepository = userRepository;
        this.operatorRepository = operatorRepository;
        this.busRepository = busRepository;
    }

    @Transactional
    public OperatorBusResponse createBus(
            String authenticatedEmail,
            OperatorBusRequest request
    ) {
        TransportOperator operator = approvedOperator(authenticatedEmail);
        validate(request);

        if (busRepository.existsByBusNumberIgnoreCase(
                request.busNumber().trim()
        )) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Bus number is already registered"
            );
        }

        if (!isBlank(request.permitNumber()) &&
                busRepository.existsByPermitNumberIgnoreCase(
                        request.permitNumber().trim()
                )) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Permit number is already registered"
            );
        }

        Bus bus = new Bus();
        bus.setBusNumber(request.busNumber());
        bus.setBusName(request.busName());
        bus.setModel(request.model());
        bus.setManufactureYear(request.manufactureYear());
        bus.setSeatCapacity(request.seatCapacity());
        bus.setBusType(request.busType());
        bus.setFuelType(request.fuelType());
        bus.setPermitNumber(
                isBlank(request.permitNumber())
                        ? null
                        : request.permitNumber()
        );
        bus.setPermitExpiryDate(request.permitExpiryDate());
        bus.setInsuranceExpiryDate(request.insuranceExpiryDate());
        bus.setStatus(BusStatus.PENDING);
        bus.setOperator(operator);
        bus.setOperatorName(operator.getName());

        try {
            return toResponse(busRepository.saveAndFlush(bus));
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Bus or permit number is already registered"
            );
        }
    }

    public List<OperatorBusResponse> getBuses(String authenticatedEmail) {
        TransportOperator operator = approvedOperator(authenticatedEmail);
        return busRepository
                .findByOperatorOrderByCreatedAtDesc(operator)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public OperatorBusResponse getBus(
            String authenticatedEmail,
            Long busId
    ) {
        TransportOperator operator = approvedOperator(authenticatedEmail);
        Bus bus = busRepository.findByIdAndOperator(busId, operator)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Bus not found"
                ));
        return toResponse(bus);
    }

    private TransportOperator approvedOperator(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Authenticated user not found"
                ));

        if (!"OPERATOR".equalsIgnoreCase(user.getRole())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Operator access is required"
            );
        }

        TransportOperator operator = operatorRepository.findByUser(user)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Operator application not found"
                ));

        if (operator.getVerificationStatus() !=
                OperatorVerificationStatus.APPROVED) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Operator application is not approved"
            );
        }
        return operator;
    }

    private void validate(OperatorBusRequest request) {
        if (request == null) badRequest("Bus details are required");
        if (isBlank(request.busNumber())) badRequest("Bus number is required");
        if (request.busNumber().trim().length() < 4) {
            badRequest("Bus number must be at least 4 characters");
        }
        if (request.busNumber().trim().length() > 50) {
            badRequest("Bus number must not exceed 50 characters");
        }
        if (isBlank(request.busName())) badRequest("Bus name is required");
        if (request.busName().trim().length() > 120) {
            badRequest("Bus name must not exceed 120 characters");
        }
        if (isBlank(request.busType())) badRequest("Bus type is required");
        if (request.busType().trim().length() > 50) {
            badRequest("Bus type must not exceed 50 characters");
        }
        if (request.seatCapacity() == null || request.seatCapacity() < 1 ||
                request.seatCapacity() > 100) {
            badRequest("Seat capacity must be between 1 and 100");
        }
        if (request.manufactureYear() != null &&
                (request.manufactureYear() < 1900 ||
                        request.manufactureYear() > Year.now().getValue())) {
            badRequest("Manufacture year must be between 1900 and the current year");
        }
        validateLength(request.model(), 100, "Model");
        validateLength(request.fuelType(), 50, "Fuel type");
        validateLength(request.permitNumber(), 100, "Permit number");
    }

    private void badRequest(String message) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void validateLength(String value, int maximum, String field) {
        if (value != null && value.trim().length() > maximum) {
            badRequest(field + " must not exceed " + maximum + " characters");
        }
    }

    private OperatorBusResponse toResponse(Bus bus) {
        return new OperatorBusResponse(
                bus.getId(), bus.getBusNumber(), bus.getBusName(),
                bus.getModel(), bus.getManufactureYear(),
                bus.getSeatCapacity(), bus.getBusType(), bus.getFuelType(),
                bus.getPermitNumber(), bus.getPermitExpiryDate(),
                bus.getInsuranceExpiryDate(), bus.getStatus().name(),
                bus.getRejectionReason(), bus.getApprovedAt(),
                bus.getCreatedAt(), bus.getUpdatedAt()
        );
    }
}
