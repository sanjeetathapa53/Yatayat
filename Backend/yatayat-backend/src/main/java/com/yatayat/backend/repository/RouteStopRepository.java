package com.yatayat.backend.repository;

import com.yatayat.backend.entity.RouteStop;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RouteStopRepository extends JpaRepository<RouteStop, Long> {
    List<RouteStop> findByRouteIdOrderByStopOrderAsc(Long routeId);
    List<RouteStop> findByRouteIdAndActiveTrueOrderByStopOrderAsc(Long routeId);
    void deleteByRouteId(Long routeId);
}
