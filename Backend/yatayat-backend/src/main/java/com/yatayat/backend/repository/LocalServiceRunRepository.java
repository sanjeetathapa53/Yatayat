package com.yatayat.backend.repository;

import com.yatayat.backend.entity.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface LocalServiceRunRepository extends JpaRepository<LocalServiceRun, Long> {
    @EntityGraph(attributePaths = {"route", "operator", "bus", "driver", "driver.user"})
    List<LocalServiceRun> findByOperatorOrderByServiceDateDescPlannedStartTimeDesc(TransportOperator operator);

    @EntityGraph(attributePaths = {"route", "operator", "bus", "driver", "driver.user"})
    Optional<LocalServiceRun> findByIdAndOperator(Long id, TransportOperator operator);

    @EntityGraph(attributePaths = {"route", "operator", "bus", "driver", "driver.user"})
    List<LocalServiceRun> findByDriverOrderByServiceDateAscPlannedStartTimeAsc(DriverProfile driver);

    @EntityGraph(attributePaths = {"route", "operator", "bus", "driver", "driver.user"})
    Optional<LocalServiceRun> findByIdAndDriver(Long id, DriverProfile driver);

    @EntityGraph(attributePaths = {"route", "operator", "bus", "driver", "driver.user"})
    @Query("""
            select run from LocalServiceRun run
            join DriverOperatorAssociation association
              on association.driver = run.driver and association.operator = run.operator
            where run.driver = :driver
              and association.status = com.yatayat.backend.entity.DriverOperatorAssociationStatus.ACTIVE
              and (
                    run.status = com.yatayat.backend.entity.LocalServiceRunStatus.IN_SERVICE
                    or (
                        run.status = com.yatayat.backend.entity.LocalServiceRunStatus.PLANNED
                        and run.serviceDate >= :today
                    )
                  )
            order by case run.status
                       when com.yatayat.backend.entity.LocalServiceRunStatus.IN_SERVICE then 0
                       else 1
                     end,
                     run.serviceDate asc,
                     run.plannedStartTime asc
            """)
    List<LocalServiceRun> findDriverOperationalRuns(
            @Param("driver") DriverProfile driver,
            @Param("today") LocalDate today
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"route", "operator", "bus", "driver", "driver.user"})
    @Query("""
            select run from LocalServiceRun run
            where run.id = :id and run.driver = :driver
            """)
    Optional<LocalServiceRun> findByIdAndDriverForOperation(
            @Param("id") Long id,
            @Param("driver") DriverProfile driver
    );

    @EntityGraph(attributePaths = {"route", "bus"})
    List<LocalServiceRun> findByStatusOrderByServiceDateAscPlannedStartTimeAsc(
            LocalServiceRunStatus status
    );

    @EntityGraph(attributePaths = {"route", "bus"})
    List<LocalServiceRun> findByStatusAndRouteIdOrderByServiceDateAscPlannedStartTimeAsc(
            LocalServiceRunStatus status,
            Long routeId
    );

    @EntityGraph(attributePaths = {"route", "bus"})
    Optional<LocalServiceRun> findByIdAndStatus(Long id, LocalServiceRunStatus status);

    @EntityGraph(attributePaths = {"route", "bus", "driver", "driver.user"})
    List<LocalServiceRun> findByOperatorAndStatusOrderByServiceDateAscPlannedStartTimeAsc(
            TransportOperator operator,
            LocalServiceRunStatus status
    );

    @EntityGraph(attributePaths = {"route", "bus", "driver", "driver.user"})
    Optional<LocalServiceRun> findByIdAndOperatorAndStatus(
            Long id,
            TransportOperator operator,
            LocalServiceRunStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select run from LocalServiceRun run
            where run.bus = :bus
              and run.status in :statuses
              and run.serviceDate = :serviceDate
              and run.plannedStartTime < :endTime
              and run.plannedEndTime > :startTime
              and (:excludedId is null or run.id <> :excludedId)
            """)
    List<LocalServiceRun> findBusConflictsForUpdate(
            @Param("bus") Bus bus,
            @Param("serviceDate") LocalDate serviceDate,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("statuses") List<LocalServiceRunStatus> statuses,
            @Param("excludedId") Long excludedId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select run from LocalServiceRun run
            where run.driver = :driver
              and run.status in :statuses
              and run.serviceDate = :serviceDate
              and run.plannedStartTime < :endTime
              and run.plannedEndTime > :startTime
              and (:excludedId is null or run.id <> :excludedId)
            """)
    List<LocalServiceRun> findDriverConflictsForUpdate(
            @Param("driver") DriverProfile driver,
            @Param("serviceDate") LocalDate serviceDate,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("statuses") List<LocalServiceRunStatus> statuses,
            @Param("excludedId") Long excludedId
    );
}
