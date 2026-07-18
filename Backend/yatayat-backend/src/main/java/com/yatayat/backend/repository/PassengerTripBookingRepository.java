package com.yatayat.backend.repository;

import com.yatayat.backend.entity.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface PassengerTripBookingRepository extends JpaRepository<PassengerTripBooking, Long> {
    boolean existsByBookingReference(String bookingReference);

    @EntityGraph(attributePaths = {"scheduledTrip", "scheduledTrip.route", "scheduledTrip.operator", "scheduledTrip.bus", "seats"})
    List<PassengerTripBooking> findByPassengerOrderByBookedAtDesc(User passenger);

    @EntityGraph(attributePaths = {"scheduledTrip", "scheduledTrip.route", "scheduledTrip.operator", "scheduledTrip.bus", "seats"})
    Optional<PassengerTripBooking> findByBookingReferenceAndPassenger(String reference, User passenger);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"scheduledTrip", "scheduledTrip.route", "scheduledTrip.operator", "scheduledTrip.bus", "seats"})
    @Query("""
            select booking from PassengerTripBooking booking
            where booking.bookingReference = :reference
              and booking.passenger.id = :passengerId
            """)
    Optional<PassengerTripBooking> findOwnedByReferenceForPayment(
            @Param("reference") String reference,
            @Param("passengerId") Long passengerId
    );

    @Query("""
            select coalesce(sum(booking.numberOfSeats), 0)
            from PassengerTripBooking booking
            where booking.scheduledTrip = :trip
              and booking.status = com.yatayat.backend.entity.BookingStatus.CONFIRMED
            """)
    Long sumConfirmedSeatsByTrip(@Param("trip") ScheduledTrip trip);
}
