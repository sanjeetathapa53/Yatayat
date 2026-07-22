package com.yatayat.backend.trip;

import com.yatayat.backend.dto.*;
import com.yatayat.backend.entity.*;
import com.yatayat.backend.payment.*;
import com.yatayat.backend.repository.*;
import com.yatayat.backend.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class KhaltiPaymentServiceTests {
    private KhaltiProperties properties;
    private KhaltiGateway gateway;
    private UserRepository users;
    private PassengerTripBookingRepository bookings;
    private BookingSeatRepository seats;
    private PaymentRepository payments;
    private PassengerTicketService tickets;
    private KhaltiPaymentService service;
    private User passenger;
    private PassengerTripBooking booking;
    private Payment payment;

    @BeforeEach
    void setUp() {
        properties = new KhaltiProperties();
        properties.setEnabled(true);
        gateway = mock(KhaltiGateway.class);
        users = mock(UserRepository.class);
        bookings = mock(PassengerTripBookingRepository.class);
        seats = mock(BookingSeatRepository.class);
        payments = mock(PaymentRepository.class);
        tickets = mock(PassengerTicketService.class);
        service = new KhaltiPaymentService(properties, gateway, users, bookings, seats, payments, tickets);
        passenger = new User("Passenger", "passenger@example.com", "9800000000", "encoded", "PASSENGER");
        passenger.setId(7L);
        booking = pendingBooking();
        payment = initiatedPayment();
        when(users.findByEmailIgnoreCase(passenger.getEmail())).thenReturn(Optional.of(passenger));
        when(bookings.findOwnedByReferenceForPayment(booking.getBookingReference(), passenger.getId()))
                .thenReturn(Optional.of(booking));
        when(seats.findWithLockByBookingOrderBySeatNumberAsc(booking)).thenReturn(booking.getSeats());
        when(payments.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(bookings.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void exactNprToPaisaConversionUsesNoFloatingPoint() {
        assertThat(KhaltiPaymentService.toPaisa(new BigDecimal("1234.56"))).isEqualTo(123456L);
        assertThatThrownBy(() -> KhaltiPaymentService.toPaisa(new BigDecimal("1.001")))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void validInitiationUsesBackendAmountStoresPidxAndReturnsOfficialPaymentUrl() {
        when(gateway.initiate(any())).thenReturn(initiation());
        ExternalPaymentInitiationResponse response = service.initiate(
                passenger.getEmail(), booking.getBookingReference());

        var request = org.mockito.ArgumentCaptor.forClass(KhaltiGateway.InitiationRequest.class);
        verify(gateway).initiate(request.capture());
        assertThat(request.getValue().amount()).isEqualTo(100000L);
        assertThat(request.getValue().purchaseOrderId()).isEqualTo(booking.getBookingReference());
        assertThat(request.getValue().returnUrl()).contains("bookingReference=");
        assertThat(response.redirectUrl()).startsWith("https://test-pay.khalti.com/");
        var saved = org.mockito.ArgumentCaptor.forClass(Payment.class);
        verify(payments).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getProviderPaymentId()).isEqualTo("test-pidx-001");
        assertThat(saved.getValue().getStatus()).isEqualTo(PaymentStatus.INITIATED);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        verifyNoInteractions(tickets);
    }

    @Test
    void malformedOrUnavailableInitiationNeverConfirmsBooking() {
        when(gateway.initiate(any())).thenReturn(new KhaltiGateway.InitiationResult("", "bad", null, null));
        assertConflictOrProviderError(() -> service.initiate(passenger.getEmail(), booking.getBookingReference()));
        when(gateway.initiate(any())).thenThrow(new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unavailable"));
        assertConflictOrProviderError(() -> service.initiate(passenger.getEmail(), booking.getBookingReference()));
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        verifyNoInteractions(tickets);
    }

    @Test
    void ownershipCancelledExpiredAndDisabledConfigurationAreRejected() {
        User other = new User("Other", "other@example.com", "", "", "PASSENGER"); other.setId(8L);
        when(users.findByEmailIgnoreCase(other.getEmail())).thenReturn(Optional.of(other));
        assertStatus(404, () -> service.initiate(other.getEmail(), booking.getBookingReference()));
        booking.setStatus(BookingStatus.CANCELLED);
        assertStatus(409, () -> service.initiate(passenger.getEmail(), booking.getBookingReference()));
        booking.setStatus(BookingStatus.EXPIRED);
        assertStatus(409, () -> service.initiate(passenger.getEmail(), booking.getBookingReference()));
        booking.setStatus(BookingStatus.PENDING_PAYMENT);
        properties.setEnabled(false);
        assertStatus(503, () -> service.initiate(passenger.getEmail(), booking.getBookingReference()));
    }

    @Test
    void confirmedInitiationIsIdempotent() {
        booking.setStatus(BookingStatus.CONFIRMED);
        payment.setStatus(PaymentStatus.SUCCESS);
        when(payments.findFirstByBookingAndStatusOrderByCreatedAtDesc(booking, PaymentStatus.SUCCESS))
                .thenReturn(Optional.of(payment));
        ExternalPaymentInitiationResponse response = service.initiate(
                passenger.getEmail(), booking.getBookingReference());
        assertThat(response.paymentStatus()).isEqualTo("SUCCESS");
        verifyNoInteractions(gateway);
    }

    @Test
    void completedLookupWithExactAmountConfirmsAndRepeatedVerificationIsIdempotent() {
        prepareVerification();
        when(gateway.lookup(payment.getProviderPaymentId())).thenReturn(completed("provider-txn-001", 100000L));
        Ticket ticket = ticket(); when(tickets.issueForConfirmedBooking(booking)).thenReturn(ticket);
        KhaltiPaymentVerificationRequest request = new KhaltiPaymentVerificationRequest(payment.getProviderPaymentId());

        ExternalPaymentVerificationResponse first = service.verify(passenger.getEmail(), booking.getBookingReference(), request);
        ExternalPaymentVerificationResponse repeated = service.verify(passenger.getEmail(), booking.getBookingReference(), request);

        assertThat(first.bookingStatus()).isEqualTo("CONFIRMED");
        assertThat(first.ticketNumber()).isEqualTo(ticket.getTicketNumber());
        assertThat(repeated.paymentStatus()).isEqualTo("SUCCESS");
        verify(gateway, times(1)).lookup(payment.getProviderPaymentId());
        verify(tickets, times(1)).scheduleAutomaticEmailAfterCommit(ticket.getTicketNumber());
    }

    @Test
    void callbackValuesCannotConfirmWithoutMatchingStoredPidx() {
        when(payments.findByBookingAndPaymentMethodAndProviderPaymentId(
                booking, PaymentMethod.KHALTI, "untrusted-pidx")).thenReturn(Optional.empty());
        assertStatus(404, () -> service.verify(passenger.getEmail(), booking.getBookingReference(),
                new KhaltiPaymentVerificationRequest("untrusted-pidx")));
        verifyNoInteractions(gateway, tickets);
    }

    @Test
    void lookupPidxMismatchWrongAmountMissingTransactionAndRefundAreRejected() {
        prepareVerification();
        when(gateway.lookup(payment.getProviderPaymentId()))
                .thenReturn(new KhaltiGateway.LookupResult("different", 100000L, "Completed", "tx", false));
        assertStatus(409, this::verifyPayment);
        when(gateway.lookup(payment.getProviderPaymentId())).thenReturn(completed("tx", 99999L));
        assertStatus(409, this::verifyPayment);
        when(gateway.lookup(payment.getProviderPaymentId())).thenReturn(completed("", 100000L));
        assertStatus(409, this::verifyPayment);
        when(gateway.lookup(payment.getProviderPaymentId()))
                .thenReturn(new KhaltiGateway.LookupResult(payment.getProviderPaymentId(), 100000L,
                        "Completed", "tx", true));
        ExternalPaymentVerificationResponse refunded = verifyPayment();
        assertThat(refunded.paymentStatus()).isEqualTo("REFUNDED");
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        verifyNoInteractions(tickets);
    }

    @Test
    void nonCompletedStatusesNeverConfirmOrIssueTicket() {
        for (String status : List.of("Pending", "Initiated", "User canceled", "Expired", "Failed")) {
            payment.setStatus(PaymentStatus.INITIATED); payment.setFailureReason(null);
            prepareVerification();
            when(gateway.lookup(payment.getProviderPaymentId()))
                    .thenReturn(new KhaltiGateway.LookupResult(payment.getProviderPaymentId(),
                            100000L, status, null, false));
            ExternalPaymentVerificationResponse response = verifyPayment();
            assertThat(response.bookingStatus()).isEqualTo("PENDING_PAYMENT");
            assertThat(response.ticketNumber()).isNull();
        }
        verifyNoInteractions(tickets);
    }

    @Test
    void duplicateProviderTransactionIsHandledWithoutTicket() {
        prepareVerification();
        when(gateway.lookup(payment.getProviderPaymentId())).thenReturn(completed("duplicate-tx", 100000L));
        when(payments.saveAndFlush(payment)).thenThrow(new DataIntegrityViolationException("duplicate"));
        assertStatus(409, this::verifyPayment);
        verifyNoInteractions(tickets);
    }

    private void prepareVerification() {
        when(payments.findByBookingAndPaymentMethodAndProviderPaymentId(
                booking, PaymentMethod.KHALTI, payment.getProviderPaymentId())).thenReturn(Optional.of(payment));
    }
    private ExternalPaymentVerificationResponse verifyPayment() {
        return service.verify(passenger.getEmail(), booking.getBookingReference(),
                new KhaltiPaymentVerificationRequest(payment.getProviderPaymentId()));
    }
    private KhaltiGateway.InitiationResult initiation() {
        return new KhaltiGateway.InitiationResult("test-pidx-001",
                "https://test-pay.khalti.com/?pidx=test-pidx-001", null, 1800L);
    }
    private KhaltiGateway.LookupResult completed(String transaction, long amount) {
        return new KhaltiGateway.LookupResult(payment.getProviderPaymentId(), amount,
                "Completed", transaction, false);
    }
    private PassengerTripBooking pendingBooking() {
        Route route = new Route(); route.setTripType(TripType.OUT_OF_VALLEY);
        ScheduledTrip trip = new ScheduledTrip(); trip.setId(10L); trip.setRoute(route);
        trip.setDepartureAt(LocalDateTime.now().plusDays(1));
        PassengerTripBooking result = new PassengerTripBooking(); result.setId(20L);
        result.setBookingReference("YAT-20260722-KHALTI"); result.setPassenger(passenger);
        result.setScheduledTrip(trip); result.setPassengerName("Passenger"); result.setPassengerPhone("9800000000");
        result.setNumberOfSeats(2); result.setFarePerSeat(new BigDecimal("500.00"));
        result.setTotalFare(new BigDecimal("1000.00")); result.setStatus(BookingStatus.PENDING_PAYMENT);
        BookingSeat first = heldSeat(result, "1A"); BookingSeat second = heldSeat(result, "1B");
        result.setSeats(List.of(first, second)); return result;
    }
    private BookingSeat heldSeat(PassengerTripBooking owner, String number) {
        BookingSeat seat = new BookingSeat(); seat.setBooking(owner); seat.setScheduledTrip(owner.getScheduledTrip());
        seat.setPassenger(passenger); seat.setSeatNumber(number); seat.setActiveSeatNumber(number);
        seat.setStatus(BookingSeatStatus.HELD); seat.setHeldAt(LocalDateTime.now());
        seat.setHoldExpiresAt(LocalDateTime.now().plusMinutes(5)); return seat;
    }
    private Payment initiatedPayment() {
        Payment result = new Payment(); result.setId(30L); result.setBooking(booking); result.setPassenger(passenger);
        result.setAmount(booking.getTotalFare()); result.setPaymentMethod(PaymentMethod.KHALTI);
        result.setStatus(PaymentStatus.INITIATED); result.setTransactionReference("KHALTI-TEST-001");
        result.setProviderPaymentId("test-pidx-001"); result.setInitiatedAt(LocalDateTime.now()); return result;
    }
    private Ticket ticket() { Ticket result = new Ticket(); result.setTicketNumber("YT-TKT-KHALTI-001"); return result; }
    private void assertStatus(int status, ThrowingCallable callable) {
        assertThatThrownBy(callable::call).isInstanceOfSatisfying(ResponseStatusException.class,
                error -> assertThat(error.getStatusCode().value()).isEqualTo(status));
    }
    private void assertConflictOrProviderError(ThrowingCallable callable) {
        assertThatThrownBy(callable::call).isInstanceOf(ResponseStatusException.class);
    }
    @FunctionalInterface private interface ThrowingCallable { Object call(); }
}
