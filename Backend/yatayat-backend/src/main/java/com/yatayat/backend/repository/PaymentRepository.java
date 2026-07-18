package com.yatayat.backend.repository;

import com.yatayat.backend.entity.*;
import org.springframework.data.jpa.repository.*;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findFirstByBookingAndStatusOrderByCreatedAtDesc(
            PassengerTripBooking booking,
            PaymentStatus status
    );

    boolean existsByTransactionReference(String transactionReference);
}
