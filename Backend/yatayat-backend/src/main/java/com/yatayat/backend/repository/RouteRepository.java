package com.yatayat.backend.repository;

import com.yatayat.backend.entity.Route;
import com.yatayat.backend.entity.RouteStatus;
import com.yatayat.backend.entity.TripType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RouteRepository extends JpaRepository<Route, Long> {
    boolean existsByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);
    boolean existsByTripTypeAndOriginIgnoreCaseAndDestinationIgnoreCase(
            TripType tripType, String origin, String destination);
    List<Route> findAllByOrderByCodeAsc();
    List<Route> findByTripTypeOrderByCodeAsc(TripType tripType);
    List<Route> findByStatusOrderByCodeAsc(RouteStatus status);
    List<Route> findByStatusAndTripTypeOrderByCodeAsc(RouteStatus status, TripType tripType);
    List<Route> findByStatusAndTripTypeAndOriginIgnoreCaseAndDestinationIgnoreCaseOrderByCodeAsc(
            RouteStatus status, TripType tripType, String origin, String destination);
    Optional<Route> findByIdAndStatusAndTripType(Long id, RouteStatus status, TripType tripType);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select route from Route route where route.id = :routeId")
    Optional<Route> findByIdForUpdate(@Param("routeId") Long routeId);
}
