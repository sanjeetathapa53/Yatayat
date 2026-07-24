package com.yatayat.backend.repository;

import com.yatayat.backend.entity.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface WalletTopUpRepository extends JpaRepository<WalletTopUp, Long> {
    boolean existsByTopUpReference(String reference);

    Optional<WalletTopUp> findFirstByWalletAndPaymentMethodAndAmountAndStatusOrderByCreatedAtDesc(
            Wallet wallet, PaymentMethod method, BigDecimal amount, PaymentStatus status);

    Optional<WalletTopUp> findByTopUpReferenceAndPassengerId(String reference, Long passengerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from WalletTopUp t where t.topUpReference = :reference and t.passenger.id = :passengerId")
    Optional<WalletTopUp> findOwnedWithLock(@Param("reference") String reference,
                                            @Param("passengerId") Long passengerId);
}
