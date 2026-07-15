package com.yatayat.backend.service;

import com.yatayat.backend.dto.OperatorDashboardResponse;
import com.yatayat.backend.entity.BusStatus;
import com.yatayat.backend.entity.OperatorVerificationStatus;
import com.yatayat.backend.entity.TransportOperator;
import com.yatayat.backend.entity.User;
import com.yatayat.backend.repository.BusRepository;
import com.yatayat.backend.repository.TransportOperatorRepository;
import com.yatayat.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OperatorDashboardService {

    private final UserRepository userRepository;
    private final TransportOperatorRepository operatorRepository;
    private final BusRepository busRepository;

    public OperatorDashboardService(
            UserRepository userRepository,
            TransportOperatorRepository operatorRepository,
            BusRepository busRepository
    ) {
        this.userRepository = userRepository;
        this.operatorRepository = operatorRepository;
        this.busRepository = busRepository;
    }

    public OperatorDashboardResponse getDashboard(String authenticatedEmail) {
        User user = userRepository
                .findByEmailIgnoreCase(authenticatedEmail)
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

        TransportOperator operator = operatorRepository
                .findByUser(user)
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

        return new OperatorDashboardResponse(
                operator.getId(),
                operator.getName(),
                operator.getRegistrationNumber(),
                operator.getEmail(),
                operator.getPhone(),
                operator.getAddress(),
                operator.getVerificationStatus().name(),
                busRepository.countByOperator(operator),
                busRepository.countByOperatorAndStatus(
                        operator,
                        BusStatus.PENDING
                ),
                busRepository.countByOperatorAndStatus(
                        operator,
                        BusStatus.ACTIVE
                ),
                busRepository.countDistinctAssignedDriversByOperator(operator),
                0,
                0
        );
    }
}
