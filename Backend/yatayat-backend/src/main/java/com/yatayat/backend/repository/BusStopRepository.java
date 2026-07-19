package com.yatayat.backend.repository;

import com.yatayat.backend.entity.BusStop;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BusStopRepository extends JpaRepository<BusStop, Long> {
    boolean existsByNormalizedName(String normalizedName);
    boolean existsByNormalizedNameAndIdNot(String normalizedName, Long id);
    List<BusStop> findAllByOrderByNameAsc();
    List<BusStop> findTop20ByActiveTrueAndNameContainingIgnoreCaseOrderByNameAsc(String query);
    List<BusStop> findTop20ByActiveTrueAndLandmarkContainingIgnoreCaseOrderByNameAsc(String query);
}
