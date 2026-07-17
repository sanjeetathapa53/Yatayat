package com.yatayat.backend.repository;

import com.yatayat.backend.entity.Route;
import com.yatayat.backend.entity.RouteStatus;
import com.yatayat.backend.entity.TripType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RouteRepository extends JpaRepository<Route, Long> {
    boolean existsByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);
    List<Route> findAllByOrderByCodeAsc();
    List<Route> findByStatusOrderByCodeAsc(RouteStatus status);
    List<Route> findByStatusAndTripTypeAndOriginIgnoreCaseAndDestinationIgnoreCaseOrderByCodeAsc(
            RouteStatus status, TripType tripType, String origin, String destination);
    Optional<Route> findByIdAndStatusAndTripType(Long id, RouteStatus status, TripType tripType);
}
