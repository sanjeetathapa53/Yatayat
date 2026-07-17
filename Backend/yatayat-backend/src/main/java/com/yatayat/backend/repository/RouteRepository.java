package com.yatayat.backend.repository;

import com.yatayat.backend.entity.Route;
import com.yatayat.backend.entity.RouteStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RouteRepository extends JpaRepository<Route, Long> {
    boolean existsByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);
    List<Route> findAllByOrderByCodeAsc();
    List<Route> findByStatusOrderByCodeAsc(RouteStatus status);
}
