package com.yatayat.backend.repository;

import com.yatayat.backend.entity.PassengerTripBooking;
import com.yatayat.backend.entity.Ticket;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    boolean existsByTicketNumber(String ticketNumber);
    boolean existsByQrTokenHash(String qrTokenHash);
    Optional<Ticket> findByBooking(PassengerTripBooking booking);
    Optional<Ticket> findByTicketNumber(String ticketNumber);

    @EntityGraph(attributePaths = {
            "booking", "booking.passenger", "booking.scheduledTrip", "booking.scheduledTrip.route",
            "booking.scheduledTrip.operator", "booking.scheduledTrip.bus", "booking.seats"
    })
    Optional<Ticket> findByTicketNumberAndBookingPassengerEmailIgnoreCase(String ticketNumber, String email);

    @EntityGraph(attributePaths = {
            "booking", "booking.passenger", "booking.scheduledTrip", "booking.scheduledTrip.route",
            "booking.scheduledTrip.operator", "booking.scheduledTrip.bus", "booking.seats"
    })
    Optional<Ticket> findByBookingBookingReferenceAndBookingPassengerEmailIgnoreCase(String bookingReference, String email);
}
