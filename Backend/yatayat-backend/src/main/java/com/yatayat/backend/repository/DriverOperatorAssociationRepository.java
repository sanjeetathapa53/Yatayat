package com.yatayat.backend.repository;

import com.yatayat.backend.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DriverOperatorAssociationRepository
        extends JpaRepository<DriverOperatorAssociation, Long> {

    List<DriverOperatorAssociation> findByOperatorOrderByInvitedAtDesc(
            TransportOperator operator
    );

    List<DriverOperatorAssociation> findByOperatorAndStatusOrderByInvitedAtDesc(
            TransportOperator operator,
            DriverOperatorAssociationStatus status
    );

    List<DriverOperatorAssociation> findByDriverAndStatusOrderByInvitedAtDesc(
            DriverProfile driver,
            DriverOperatorAssociationStatus status
    );

    Optional<DriverOperatorAssociation> findByDriverAndOperator(
            DriverProfile driver,
            TransportOperator operator
    );

    Optional<DriverOperatorAssociation> findByDriverAndStatus(
            DriverProfile driver,
            DriverOperatorAssociationStatus status
    );

    Optional<DriverOperatorAssociation> findByIdAndDriver(
            Long id,
            DriverProfile driver
    );

    long countByOperatorAndStatus(
            TransportOperator operator,
            DriverOperatorAssociationStatus status
    );
}
