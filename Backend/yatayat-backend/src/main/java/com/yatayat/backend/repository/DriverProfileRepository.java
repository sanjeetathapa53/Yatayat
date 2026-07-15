package com.yatayat.backend.repository;

import com.yatayat.backend.entity.DriverProfile;
import com.yatayat.backend.entity.DriverVerificationStatus;
import com.yatayat.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DriverProfileRepository
        extends JpaRepository<DriverProfile, Long> {

    Optional<DriverProfile> findByUser(User user);

    Optional<DriverProfile> findByLicenseNumber(String licenseNumber);

    Optional<DriverProfile> findByCitizenshipNumber(
            String citizenshipNumber
    );

    List<DriverProfile>
    findByVerificationStatusOrderBySubmittedAtAsc(
            DriverVerificationStatus verificationStatus
    );
}