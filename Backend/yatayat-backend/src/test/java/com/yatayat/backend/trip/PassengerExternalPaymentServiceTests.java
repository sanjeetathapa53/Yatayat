package com.yatayat.backend.trip;

import com.yatayat.backend.dto.*;
import com.yatayat.backend.entity.*;
import com.yatayat.backend.payment.ExternalPaymentVerifier;
import com.yatayat.backend.repository.*;
import com.yatayat.backend.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PassengerExternalPaymentServiceTests {
    private UserRepository users;
    private PassengerTripBookingRepository bookings;
    private BookingSeatRepository seats;
    private PaymentRepository payments;
    private PassengerTicketService tickets;
    private ExternalPaymentVerifier verifier;
    private PassengerExternalPaymentService service;
    private User passenger;
    private PassengerTripBooking booking;
    private Payment payment;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        bookings = mock(PassengerTripBookingRepository.class);
        seats = mock(BookingSeatRepository.class);
        payments = mock(PaymentRepository.class);
        tickets = mock(PassengerTicketService.class);
        verifier = mock(ExternalPaymentVerifier.class);
        service = new PassengerExternalPaymentService(users, bookings, seats, payments, tickets, verifier);

        passenger = new User("Passenger", "passenger@example.com", "9800000000", "encoded", "PASSENGER");
        passenger.setId(7L);
        booking = pendingBooking();
        payment = initiatedPayment(PaymentMethod.ESEWA);
        when(users.findByEmailIgnoreCase(passenger.getEmail())).thenReturn(Optional.of(passenger));
        when(bookings.findOwnedByReferenceForPayment(booking.getBookingReference(), passenger.getId()))
                .thenReturn(Optional.of(booking));
        when(seats.findByBookingOrderBySeatNumberAsc(booking)).thenReturn(booking.getSeats());
        when(seats.findWithLockByBookingOrderBySeatNumberAsc(booking)).thenReturn(booking.getSeats());
        when(payments.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(payments.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(bookings.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(verifier.isConfigured(any())).thenReturn(false);
    }

    @Test
    void initiationUsesBackendBookingTotalAndDoesNotConfirmBooking() {
        when(payments.findFirstByBookingAndPaymentMethodAndStatusOrderByCreatedAtDesc(
                booking, PaymentMethod.ESEWA, PaymentStatus.INITIATED)).thenReturn(Optional.empty());

        ExternalPaymentInitiationResponse response = service.initiate(
                passenger.getEmail(), booking.getBookingReference(), PaymentMethod.ESEWA);

        assertThat(response.amount()).isEqualByComparingTo("1000.00");
        assertThat(response.paymentStatus()).isEqualTo("INITIATED");
        assertThat(response.providerConfigured()).isFalse();
        assertThat(response.redirectUrl()).isNull();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        verifyNoInteractions(tickets);
    }

    @Test
    void repeatedInitiationReturnsExistingAttempt() {
        when(payments.findFirstByBookingAndPaymentMethodAndStatusOrderByCreatedAtDesc(
                booking, PaymentMethod.ESEWA, PaymentStatus.INITIATED)).thenReturn(Optional.of(payment));

        ExternalPaymentInitiationResponse response = service.initiate(
                passenger.getEmail(), booking.getBookingReference(), PaymentMethod.ESEWA);

        assertThat(response.paymentReference()).isEqualTo(payment.getTransactionReference());
        verify(payments, never()).saveAndFlush(any());
    }

    @Test
    void unverifiedExternalPaymentNeverConfirmsOrIssuesTicket() {
        when(payments.findByBookingAndPaymentMethodAndTransactionReference(
                booking, PaymentMethod.ESEWA, payment.getTransactionReference()))
                .thenReturn(Optional.of(payment));
        when(verifier.verify(any(), anyString(), anyString(), anyString(), any()))
                .thenReturn(ExternalPaymentVerifier.VerificationResult.unavailable("Not configured."));

        assertThatThrownBy(() -> service.verify(passenger.getEmail(), booking.getBookingReference(),
                PaymentMethod.ESEWA, new ExternalPaymentVerificationRequest(
                        payment.getTransactionReference(), "provider-123")))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode().value()).isEqualTo(409));

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.INITIATED);
        verifyNoInteractions(tickets);
    }

    @Test
    void verifiedPaymentConfirmsOnceAndRepeatedVerificationDoesNotScheduleAnotherTicket() {
        when(payments.findByBookingAndPaymentMethodAndTransactionReference(
                booking, PaymentMethod.ESEWA, payment.getTransactionReference()))
                .thenReturn(Optional.of(payment));
        when(verifier.verify(any(), anyString(), anyString(), anyString(), any()))
                .thenReturn(new ExternalPaymentVerifier.VerificationResult(
                        true, booking.getTotalFare(), null));
        Ticket ticket = new Ticket();
        ticket.setTicketNumber("YT-TKT-TEST-001");
        when(tickets.issueForConfirmedBooking(booking)).thenReturn(ticket);
        ExternalPaymentVerificationRequest request = new ExternalPaymentVerificationRequest(
                payment.getTransactionReference(), "provider-success-123");

        ExternalPaymentVerificationResponse first = service.verify(passenger.getEmail(),
                booking.getBookingReference(), PaymentMethod.ESEWA, request);
        ExternalPaymentVerificationResponse repeated = service.verify(passenger.getEmail(),
                booking.getBookingReference(), PaymentMethod.ESEWA, request);

        assertThat(first.bookingStatus()).isEqualTo("CONFIRMED");
        assertThat(repeated.ticketNumber()).isEqualTo("YT-TKT-TEST-001");
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        verify(verifier, times(1)).verify(any(), anyString(), anyString(), anyString(), any());
        verify(tickets, times(1)).scheduleAutomaticEmailAfterCommit("YT-TKT-TEST-001");
    }

    @Test
    void anotherPassengerCannotAccessPaymentAttempt() {
        User other = new User("Other", "other@example.com", "", "", "PASSENGER");
        other.setId(8L);
        when(users.findByEmailIgnoreCase(other.getEmail())).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> service.initiate(other.getEmail(), booking.getBookingReference(),
                PaymentMethod.KHALTI))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode().value()).isEqualTo(404));
    }

    @Test
    void duplicateProviderTransactionIsRejectedWithoutIssuingTicket() {
        when(payments.findByBookingAndPaymentMethodAndTransactionReference(
                booking, PaymentMethod.ESEWA, payment.getTransactionReference()))
                .thenReturn(Optional.of(payment));
        when(verifier.verify(any(), anyString(), anyString(), anyString(), any()))
                .thenReturn(new ExternalPaymentVerifier.VerificationResult(
                        true, booking.getTotalFare(), null));
        when(payments.save(payment)).thenThrow(
                new DataIntegrityViolationException("duplicate provider transaction"));

        assertThatThrownBy(() -> service.verify(passenger.getEmail(), booking.getBookingReference(),
                PaymentMethod.ESEWA, new ExternalPaymentVerificationRequest(
                        payment.getTransactionReference(), "already-used-provider-id")))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode().value()).isEqualTo(409));

        verifyNoInteractions(tickets);
    }

    private PassengerTripBooking pendingBooking() {
        User operatorUser = new User("Operator", "operator@example.com", "", "", "OPERATOR");
        TransportOperator operator = new TransportOperator();
        operator.setName("Safe Travels"); operator.setUser(operatorUser);
        Route route = new Route(); route.setName("Kathmandu to Pokhara");
        route.setOrigin("Kathmandu"); route.setDestination("Pokhara"); route.setTripType(TripType.OUT_OF_VALLEY);
        Bus bus = new Bus(); bus.setBusNumber("BA-1-KHA-1000"); bus.setBusName("Mountain Express");
        ScheduledTrip trip = new ScheduledTrip(); trip.setId(10L); trip.setRoute(route);
        trip.setOperator(operator); trip.setBus(bus); trip.setDepartureAt(LocalDateTime.now().plusDays(1));
        PassengerTripBooking result = new PassengerTripBooking(); result.setId(20L);
        result.setBookingReference("YAT-20260722-PAY001"); result.setPassenger(passenger);
        result.setScheduledTrip(trip); result.setPassengerName("Passenger"); result.setPassengerPhone("9800000000");
        result.setNumberOfSeats(2); result.setFarePerSeat(new BigDecimal("500.00"));
        result.setTotalFare(new BigDecimal("1000.00")); result.setStatus(BookingStatus.PENDING_PAYMENT);
        BookingSeat first = heldSeat(result, "1A"); BookingSeat second = heldSeat(result, "1B");
        result.setSeats(List.of(first, second));
        return result;
    }

    private BookingSeat heldSeat(PassengerTripBooking owner, String number) {
        BookingSeat seat = new BookingSeat(); seat.setBooking(owner); seat.setScheduledTrip(owner.getScheduledTrip());
        seat.setPassenger(passenger); seat.setSeatNumber(number); seat.setActiveSeatNumber(number);
        seat.setStatus(BookingSeatStatus.HELD); seat.setHeldAt(LocalDateTime.now());
        seat.setHoldExpiresAt(LocalDateTime.now().plusMinutes(5)); return seat;
    }

    private Payment initiatedPayment(PaymentMethod method) {
        Payment result = new Payment(); result.setId(30L); result.setBooking(booking);
        result.setPassenger(passenger); result.setAmount(booking.getTotalFare()); result.setPaymentMethod(method);
        result.setStatus(PaymentStatus.INITIATED); result.setTransactionReference("ESEWA-20260722-ABC12345");
        result.setInitiatedAt(LocalDateTime.now()); return result;
    }
}
