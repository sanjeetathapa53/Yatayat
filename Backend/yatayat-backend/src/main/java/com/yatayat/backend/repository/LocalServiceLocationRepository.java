package com.yatayat.backend.repository;

import com.yatayat.backend.entity.LocalServiceLocation;
import com.yatayat.backend.entity.LocalServiceRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LocalServiceLocationRepository extends JpaRepository<LocalServiceLocation, Long> {
    Optional<LocalServiceLocation> findByRun(LocalServiceRun run);
    long countByRun(LocalServiceRun run);
    List<LocalServiceLocation> findByRunIn(List<LocalServiceRun> runs);
}
