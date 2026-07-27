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
    @EntityGraph(attributePaths = {"route", "operator", "bus", "driver", "driver.user"})
    @Query("""
            select trip from ScheduledTrip trip
            where trip.id = :tripId
            """)
    Optional<ScheduledTrip> findByIdForOperation(@Param("tripId") Long tripId);

    @EntityGraph(attributePaths = {"route", "operator", "bus", "driver", "driver.user"})
    @Query("""
            select trip from ScheduledTrip trip
            where trip.driver = :driver
              and trip.status in :statuses
            order by case trip.status
                       when com.yatayat.backend.entity.TripStatus.IN_PROGRESS then 0
                       when com.yatayat.backend.entity.TripStatus.BOARDING then 1
                       else 2
                     end,
                     trip.departureAt asc
            """)
    List<ScheduledTrip> findDriverOperationalTrips(
            @Param("driver") DriverProfile driver,
            @Param("statuses") List<TripStatus> statuses
    );

    @EntityGraph(attributePaths = {"route", "operator", "bus", "driver", "driver.user"})
    @Query("""
            select trip from ScheduledTrip trip
            where trip.operator = :operator
              and trip.status in :statuses
            order by trip.departureAt asc
            """)
    List<ScheduledTrip> findOperatorLiveTrips(
            @Param("operator") TransportOperator operator,
            @Param("statuses") List<TripStatus> statuses
    );

    @EntityGraph(attributePaths = {"route", "bus"})
    List<ScheduledTrip> findByStatusOrderByDepartureAtAsc(TripStatus status);

    @EntityGraph(attributePaths = {"route", "bus"})
    @Query("select trip from ScheduledTrip trip where trip.id = :tripId")
    Optional<ScheduledTrip> findByIdForTracking(@Param("tripId") Long tripId);

    @EntityGraph(attributePaths = {"route", "operator", "bus", "driver", "driver.user"})
    @Query("""
            select trip from ScheduledTrip trip
            where trip.status = com.yatayat.backend.entity.TripStatus.IN_PROGRESS
              and trip.operator.verificationStatus = com.yatayat.backend.entity.OperatorVerificationStatus.APPROVED
            order by trip.departureAt asc
            """)
    List<ScheduledTrip> findAdminLiveTrips();

    @EntityGraph(attributePaths = {"route", "operator", "bus", "driver", "driver.user"})
    @Query("select trip from ScheduledTrip trip where trip.id = :tripId")
    Optional<ScheduledTrip> findByIdForAdminTracking(@Param("tripId") Long tripId);

    @EntityGraph(attributePaths = {"route", "operator", "bus", "driver"})
    @Query("""
            select distinct trip from ScheduledTrip trip
            join DriverOperatorAssociation association
              on association.driver = trip.driver and association.operator = trip.operator
            where lower(trip.route.origin) = lower(:origin)
              and lower(trip.route.destination) = lower(:destination)
              and trip.departureAt > :now
              and trip.status in :statuses
              and trip.route.status = com.yatayat.backend.entity.RouteStatus.ACTIVE
              and trip.operator.verificationStatus = com.yatayat.backend.entity.OperatorVerificationStatus.APPROVED
              and trip.bus.status = com.yatayat.backend.entity.BusStatus.APPROVED
              and trip.driver.verificationStatus = com.yatayat.backend.entity.DriverVerificationStatus.APPROVED
              and association.status = com.yatayat.backend.entity.DriverOperatorAssociationStatus.ACTIVE
              and (:fromTime is null or trip.departureAt >= :fromTime)
              and (:toTime is null or trip.departureAt < :toTime)
            order by trip.departureAt asc
            """)
    List<ScheduledTrip> searchPassengerVisible(
            @Param("origin") String origin,
            @Param("destination") String destination,
            @Param("now") LocalDateTime now,
            @Param("statuses") List<TripStatus> statuses,
            @Param("fromTime") LocalDateTime fromTime,
            @Param("toTime") LocalDateTime toTime
    );

    @EntityGraph(attributePaths = {"route", "operator", "bus", "driver"})
    @Query("""
            select distinct trip from ScheduledTrip trip
            join DriverOperatorAssociation association
              on association.driver = trip.driver and association.operator = trip.operator
            where trip.id = :tripId
              and trip.departureAt > :now
              and trip.status in :statuses
              and trip.route.status = com.yatayat.backend.entity.RouteStatus.ACTIVE
              and trip.operator.verificationStatus = com.yatayat.backend.entity.OperatorVerificationStatus.APPROVED
              and trip.bus.status = com.yatayat.backend.entity.BusStatus.APPROVED
              and trip.driver.verificationStatus = com.yatayat.backend.entity.DriverVerificationStatus.APPROVED
              and association.status = com.yatayat.backend.entity.DriverOperatorAssociationStatus.ACTIVE
            """)
    Optional<ScheduledTrip> findPassengerVisibleById(
            @Param("tripId") Long tripId,
            @Param("now") LocalDateTime now,
            @Param("statuses") List<TripStatus> statuses
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"route", "operator", "bus", "driver"})
    @Query("""
            select distinct trip from ScheduledTrip trip
            join DriverOperatorAssociation association
              on association.driver = trip.driver and association.operator = trip.operator
            where trip.id = :tripId
              and trip.departureAt > :now
              and trip.status in :statuses
              and trip.route.status = com.yatayat.backend.entity.RouteStatus.ACTIVE
              and trip.operator.verificationStatus = com.yatayat.backend.entity.OperatorVerificationStatus.APPROVED
              and trip.bus.status = com.yatayat.backend.entity.BusStatus.APPROVED
              and trip.driver.verificationStatus = com.yatayat.backend.entity.DriverVerificationStatus.APPROVED
              and association.status = com.yatayat.backend.entity.DriverOperatorAssociationStatus.ACTIVE
            """)
    Optional<ScheduledTrip> findPassengerVisibleByIdForUpdate(
            @Param("tripId") Long tripId,
            @Param("now") LocalDateTime now,
            @Param("statuses") List<TripStatus> statuses
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select trip from ScheduledTrip trip
            where trip.bus = :bus
              and trip.status in (
                  com.yatayat.backend.entity.TripStatus.SCHEDULED,
                  com.yatayat.backend.entity.TripStatus.BOARDING,
                  com.yatayat.backend.entity.TripStatus.IN_PROGRESS
              )
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
              and trip.status in (
                  com.yatayat.backend.entity.TripStatus.SCHEDULED,
                  com.yatayat.backend.entity.TripStatus.BOARDING,
                  com.yatayat.backend.entity.TripStatus.IN_PROGRESS
              )
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
