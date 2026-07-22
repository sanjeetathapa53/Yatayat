package com.yatayat.backend.repository;

import com.yatayat.backend.entity.Bus;
import com.yatayat.backend.entity.BusStatus;
import com.yatayat.backend.entity.DriverProfile;
import com.yatayat.backend.entity.TransportOperator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BusRepository
        extends JpaRepository<Bus, Long> {

    Optional<Bus> findByBusNumberIgnoreCase(String busNumber);

    Optional<Bus> findByPermitNumberIgnoreCase(String permitNumber);

    List<Bus> findByStatusOrderByCreatedAtDesc(BusStatus status);

    List<Bus> findAllByOrderByCreatedAtDesc();

    List<Bus> findByAssignedDriver(DriverProfile assignedDriver);

    List<Bus> findByOperatorAndAssignedDriver(
            TransportOperator operator,
            DriverProfile assignedDriver
    );

    boolean existsByBusNumberIgnoreCase(String busNumber);

    boolean existsByPermitNumberIgnoreCase(String permitNumber);

    long countByOperator(TransportOperator operator);

    long countByOperatorAndStatus(
            TransportOperator operator,
            BusStatus status
    );

    @Query("""
            select count(distinct bus.assignedDriver.id)
            from Bus bus
            where bus.operator = :operator
              and bus.assignedDriver is not null
            """)
    long countDistinctAssignedDriversByOperator(
            @Param("operator") TransportOperator operator
    );

    List<Bus> findByOperatorOrderByCreatedAtDesc(
            TransportOperator operator
    );

    Optional<Bus> findByIdAndOperator(
            Long id,
            TransportOperator operator
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select bus from Bus bus where bus.id = :id and bus.operator = :operator")
    Optional<Bus> findLockedByIdAndOperator(
            @Param("id") Long id,
            @Param("operator") TransportOperator operator
    );
}
