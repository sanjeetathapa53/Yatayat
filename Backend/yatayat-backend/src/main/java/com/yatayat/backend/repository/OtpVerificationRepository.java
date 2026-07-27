package com.yatayat.backend.repository;

import com.yatayat.backend.entity.OtpPurpose;
import com.yatayat.backend.entity.OtpVerification;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OtpVerification o where o.normalizedEmail = :email and o.purpose = :purpose")
    Optional<OtpVerification> findForUpdate(@Param("email") String email,
                                            @Param("purpose") OtpPurpose purpose);
}
