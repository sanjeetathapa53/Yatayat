package com.yatayat.backend.repository;

import com.yatayat.backend.entity.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface PassengerTripBookingRepository extends JpaRepository<PassengerTripBooking, Long> {
    boolean existsByBookingReference(String bookingReference);

    @EntityGraph(attributePaths = {"scheduledTrip", "scheduledTrip.route", "scheduledTrip.operator", "scheduledTrip.bus"})
    List<PassengerTripBooking> findByPassengerOrderByBookedAtDesc(User passenger);

    @EntityGraph(attributePaths = {"scheduledTrip", "scheduledTrip.route", "scheduledTrip.operator", "scheduledTrip.bus"})
    Optional<PassengerTripBooking> findByBookingReferenceAndPassenger(String reference, User passenger);

    @Query("""
            select coalesce(sum(booking.numberOfSeats), 0)
            from PassengerTripBooking booking
            where booking.scheduledTrip = :trip
              and booking.status = com.yatayat.backend.entity.BookingStatus.CONFIRMED
            """)
    Long sumConfirmedSeatsByTrip(@Param("trip") ScheduledTrip trip);
}
