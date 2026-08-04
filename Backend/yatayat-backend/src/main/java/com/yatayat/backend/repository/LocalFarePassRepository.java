package com.yatayat.backend.repository;

import com.yatayat.backend.entity.LocalFarePass;
import com.yatayat.backend.entity.Route;
import com.yatayat.backend.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LocalFarePassRepository extends JpaRepository<LocalFarePass, Long> {
    boolean existsByPassNumber(String passNumber);
    boolean existsByRoute(Route route);

    @EntityGraph(attributePaths = {"route", "boardingStop", "destinationStop", "walletTransaction"})
    List<LocalFarePass> findByPassengerOrderByIssuedAtDesc(User passenger);

    @EntityGraph(attributePaths = {"route", "boardingStop", "destinationStop", "walletTransaction"})
    Optional<LocalFarePass> findByPassNumberAndPassenger(String passNumber, User passenger);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"route", "passenger", "boardingStop", "destinationStop"})
    @Query("select pass from LocalFarePass pass where pass.passNumber = :passNumber")
    Optional<LocalFarePass> findByPassNumberForValidation(@Param("passNumber") String passNumber);
}
