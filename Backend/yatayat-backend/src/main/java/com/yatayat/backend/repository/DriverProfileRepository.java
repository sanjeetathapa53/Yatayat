package com.yatayat.backend.repository;

import com.yatayat.backend.entity.DriverProfile;
import com.yatayat.backend.entity.DriverVerificationStatus;
import com.yatayat.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface DriverProfileRepository
        extends JpaRepository<DriverProfile, Long> {

    Optional<DriverProfile> findByUser(User user);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<DriverProfile> findLockedById(Long id);

    Optional<DriverProfile> findByLicenseNumber(String licenseNumber);

    Optional<DriverProfile> findByCitizenshipNumber(
            String citizenshipNumber
    );

    List<DriverProfile>
    findByVerificationStatusOrderBySubmittedAtAsc(
            DriverVerificationStatus verificationStatus
    );
}
