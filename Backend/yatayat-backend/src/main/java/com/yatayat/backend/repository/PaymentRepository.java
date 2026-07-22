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

    boolean existsByBookingAndStatus(PassengerTripBooking booking, PaymentStatus status);

    Optional<Payment> findFirstByBookingAndPaymentMethodAndStatusOrderByCreatedAtDesc(
            PassengerTripBooking booking, PaymentMethod paymentMethod, PaymentStatus status);

    Optional<Payment> findByBookingAndPaymentMethodAndTransactionReference(
            PassengerTripBooking booking, PaymentMethod paymentMethod, String transactionReference);
}
