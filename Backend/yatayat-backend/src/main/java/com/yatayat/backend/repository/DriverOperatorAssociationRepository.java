package com.yatayat.backend.repository;

import com.yatayat.backend.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select association from DriverOperatorAssociation association " +
            "where association.id = :id and association.operator = :operator")
    Optional<DriverOperatorAssociation> findLockedByIdAndOperator(
            @Param("id") Long id,
            @Param("operator") TransportOperator operator
    );

    long countByOperatorAndStatus(
            TransportOperator operator,
            DriverOperatorAssociationStatus status
    );
}
