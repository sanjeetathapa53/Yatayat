package com.yatayat.backend.repository;

import com.yatayat.backend.entity.ScheduledTrip;
import com.yatayat.backend.entity.TripLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface TripLocationRepository extends JpaRepository<TripLocation, Long> {
    Optional<TripLocation> findByTrip(ScheduledTrip trip);
    long countByTrip(ScheduledTrip trip);
    List<TripLocation> findByTripIn(List<ScheduledTrip> trips);
}
