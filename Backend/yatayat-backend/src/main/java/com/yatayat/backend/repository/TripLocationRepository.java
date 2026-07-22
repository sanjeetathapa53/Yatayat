package com.yatayat.backend.repository;

import com.yatayat.backend.entity.ScheduledTrip;
import com.yatayat.backend.entity.TripLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TripLocationRepository extends JpaRepository<TripLocation, Long> {
    Optional<TripLocation> findByTrip(ScheduledTrip trip);
    long countByTrip(ScheduledTrip trip);
}
