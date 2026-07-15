package com.yatayat.backend.repository;

import com.yatayat.backend.entity.Bus;
import com.yatayat.backend.entity.BusStatus;
import com.yatayat.backend.entity.DriverProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BusRepository
        extends JpaRepository<Bus, Long> {

    Optional<Bus> findByBusNumberIgnoreCase(String busNumber);

    Optional<Bus> findByPermitNumberIgnoreCase(String permitNumber);

    List<Bus> findByStatusOrderByCreatedAtDesc(BusStatus status);

    List<Bus> findAllByOrderByCreatedAtDesc();

    List<Bus> findByAssignedDriver(DriverProfile assignedDriver);

    boolean existsByBusNumberIgnoreCase(String busNumber);

    boolean existsByPermitNumberIgnoreCase(String permitNumber);
}