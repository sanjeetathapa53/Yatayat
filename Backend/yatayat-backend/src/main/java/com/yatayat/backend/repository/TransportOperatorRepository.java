package com.yatayat.backend.repository;

import com.yatayat.backend.entity.OperatorVerificationStatus;
import com.yatayat.backend.entity.TransportOperator;
import com.yatayat.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransportOperatorRepository
        extends JpaRepository<TransportOperator, Long> {

    Optional<TransportOperator> findByUser(User user);

    Optional<TransportOperator> findByUserId(Long userId);

    Optional<TransportOperator>
    findByRegistrationNumberIgnoreCase(
            String registrationNumber
    );

    Optional<TransportOperator>
    findByEmailIgnoreCase(String email);

    boolean existsByUser(User user);

    boolean existsByRegistrationNumberIgnoreCase(
            String registrationNumber
    );

    boolean existsByEmailIgnoreCase(String email);

    List<TransportOperator>
    findByVerificationStatusOrderByCreatedAtDesc(
            OperatorVerificationStatus verificationStatus
    );

    List<TransportOperator>
    findAllByOrderByCreatedAtDesc();
}