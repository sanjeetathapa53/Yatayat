package com.yatayat.backend.repository;

import com.yatayat.backend.entity.PassengerTripBooking;
import com.yatayat.backend.entity.BookingStatus;
import com.yatayat.backend.entity.ScheduledTrip;
import com.yatayat.backend.entity.Ticket;
import com.yatayat.backend.entity.TicketStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    boolean existsByTicketNumber(String ticketNumber);
    boolean existsByQrTokenHash(String qrTokenHash);
    Optional<Ticket> findByBooking(PassengerTripBooking booking);
    Optional<Ticket> findByTicketNumber(String ticketNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {
            "booking", "booking.passenger", "booking.scheduledTrip", "booking.scheduledTrip.route",
            "booking.scheduledTrip.operator", "booking.scheduledTrip.bus", "booking.scheduledTrip.driver",
            "booking.seats"
    })
    @Query("select ticket from Ticket ticket where ticket.ticketNumber = :ticketNumber")
    Optional<Ticket> findByTicketNumberForValidation(@Param("ticketNumber") String ticketNumber);

    @EntityGraph(attributePaths = {
            "booking", "booking.passenger", "booking.scheduledTrip", "booking.scheduledTrip.route",
            "booking.scheduledTrip.operator", "booking.scheduledTrip.bus", "booking.seats"
    })
    List<Ticket> findByBookingScheduledTripAndBookingStatusOrderByBookingPassengerNameAsc(
            ScheduledTrip trip,
            BookingStatus status
    );

    long countByBookingScheduledTripAndStatus(ScheduledTrip trip, TicketStatus status);

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
