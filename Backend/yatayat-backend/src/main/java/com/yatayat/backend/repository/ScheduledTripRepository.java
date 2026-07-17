package com.yatayat.backend.repository;

import com.yatayat.backend.entity.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ScheduledTripRepository extends JpaRepository<ScheduledTrip, Long> {

    List<ScheduledTrip> findByOperatorOrderByDepartureAtDesc(TransportOperator operator);
    Optional<ScheduledTrip> findByIdAndOperator(Long id, TransportOperator operator);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select trip from ScheduledTrip trip
            where trip.bus = :bus
              and trip.status <> com.yatayat.backend.entity.TripStatus.CANCELLED
              and trip.departureAt < :arrival
              and trip.estimatedArrivalAt > :departure
              and (:excludedId is null or trip.id <> :excludedId)
            """)
    List<ScheduledTrip> findBusConflictsForUpdate(
            @Param("bus") Bus bus,
            @Param("departure") LocalDateTime departure,
            @Param("arrival") LocalDateTime arrival,
            @Param("excludedId") Long excludedId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select trip from ScheduledTrip trip
            where trip.driver = :driver
              and trip.status <> com.yatayat.backend.entity.TripStatus.CANCELLED
              and trip.departureAt < :arrival
              and trip.estimatedArrivalAt > :departure
              and (:excludedId is null or trip.id <> :excludedId)
            """)
    List<ScheduledTrip> findDriverConflictsForUpdate(
            @Param("driver") DriverProfile driver,
            @Param("departure") LocalDateTime departure,
            @Param("arrival") LocalDateTime arrival,
            @Param("excludedId") Long excludedId
    );
}
