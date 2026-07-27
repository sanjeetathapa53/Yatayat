package com.yatayat.backend.trip;

import com.yatayat.backend.dto.TicketResponse;
import com.yatayat.backend.entity.*;
import com.yatayat.backend.repository.PaymentRepository;
import com.yatayat.backend.repository.TicketRepository;
import com.yatayat.backend.repository.UserRepository;
import com.yatayat.backend.service.EmailService;
import com.yatayat.backend.service.NotificationService;
import com.yatayat.backend.service.PassengerTicketService;
import com.yatayat.backend.service.TicketPdfService;
import com.yatayat.backend.service.TicketQrTokenService;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PassengerTicketServiceTests {
    @Mock private TicketRepository ticketRepository;
    @Mock private UserRepository userRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private TicketPdfService ticketPdfService;
    @Mock private EmailService emailService;
    @Mock private NotificationService notificationService;

    private PassengerTicketService service;
    private TicketQrTokenService qrTokens;
    private User passenger;
    private PassengerTripBooking booking;

    @BeforeEach
    void setUp() {
        qrTokens = new TicketQrTokenService(
                "test-only-ticket-qr-secret-at-least-32-characters");
        service = new PassengerTicketService(ticketRepository, userRepository, paymentRepository,
                ticketPdfService, emailService, notificationService,
                qrTokens);
        passenger = new User("Passenger A", "a@example.com", "9800000001", "encoded", "PASSENGER");
        passenger.setId(1L);
        booking = confirmedBooking();
    }

    @Test
    void confirmedBookingCreatesOneValidTicket() {
        when(ticketRepository.findByBooking(booking)).thenReturn(Optional.empty());
        when(ticketRepository.existsByTicketNumber(anyString())).thenReturn(false);
        when(ticketRepository.saveAndFlush(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Ticket ticket = service.issueForConfirmedBooking(booking);

        assertThat(ticket.getTicketNumber()).startsWith("YT-TKT-");
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.VALID);
        assertThat(ticket.getBooking()).isEqualTo(booking);
        assertThat(ticket.getQrTokenHash()).hasSize(64);
        verify(ticketRepository).saveAndFlush(any(Ticket.class));
        verify(notificationService).ticketQrGenerated(booking, ticket);
    }

    @Test
    void newlyIssuedTicketStoresOnlyHashAndReturnsRawQrToken() {
        when(ticketRepository.findByBooking(booking)).thenReturn(Optional.empty());
        when(ticketRepository.existsByTicketNumber(anyString())).thenReturn(false);
        when(ticketRepository.saveAndFlush(any(Ticket.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        Ticket created = service.issueForConfirmedBooking(booking);
        when(userRepository.findByEmailIgnoreCase(passenger.getEmail()))
                .thenReturn(Optional.of(passenger));
        when(ticketRepository.findByTicketNumberAndBookingPassengerEmailIgnoreCase(
                created.getTicketNumber(), passenger.getEmail())).thenReturn(Optional.of(created));

        TicketResponse response =
                service.getByTicketNumber(passenger.getEmail(), created.getTicketNumber());
        String rawToken = qrTokens.rawToken(created.getTicketNumber());

        assertThat(created.getQrTokenHash()).isEqualTo(qrTokens.storedHash(created.getTicketNumber()));
        assertThat(created.getQrTokenHash()).isNotEqualTo(rawToken);
        assertThat(response.qrPayload()).contains("\"token\":\"" + rawToken + "\"");
        assertThat(response.qrPayload()).doesNotContain(created.getQrTokenHash());
    }

    @Test
    void passengerReadPersistsCancelledAndExpiredLifecycle() {
        Ticket cancelled = ticket(booking);
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(LocalDateTime.now().minusMinutes(1));
        when(userRepository.findByEmailIgnoreCase(passenger.getEmail()))
                .thenReturn(Optional.of(passenger));
        when(ticketRepository.findByTicketNumberAndBookingPassengerEmailIgnoreCase(
                cancelled.getTicketNumber(), passenger.getEmail()))
                .thenReturn(Optional.of(cancelled));

        TicketResponse cancelledResponse =
                service.getByTicketNumber(passenger.getEmail(), cancelled.getTicketNumber());

        assertThat(cancelledResponse.ticketStatus()).isEqualTo("CANCELLED");
        assertThat(cancelled.getCancelledAt()).isEqualTo(booking.getCancelledAt());
        verify(ticketRepository).saveAndFlush(cancelled);

        reset(ticketRepository);
        booking.setStatus(BookingStatus.CONFIRMED);
        Ticket expired = ticket(booking);
        expired.setValidUntil(LocalDateTime.now().minusMinutes(1));
        when(ticketRepository.findByTicketNumberAndBookingPassengerEmailIgnoreCase(
                expired.getTicketNumber(), passenger.getEmail())).thenReturn(Optional.of(expired));

        TicketResponse expiredResponse =
                service.getByTicketNumber(passenger.getEmail(), expired.getTicketNumber());

        assertThat(expiredResponse.ticketStatus()).isEqualTo("EXPIRED");
        verify(ticketRepository).saveAndFlush(expired);
    }

    @Test
    void existingTicketIsReusedForIdempotency() {
        Ticket existing = ticket(booking);
        when(ticketRepository.findByBooking(booking)).thenReturn(Optional.of(existing));

        Ticket result = service.issueForConfirmedBooking(booking);

        assertThat(result).isSameAs(existing);
        verify(ticketRepository, never()).saveAndFlush(any());
        verify(notificationService).ticketQrGenerated(booking, existing);
    }

    @Test
    void pendingBookingCannotIssueTicket() {
        booking.setStatus(BookingStatus.PENDING_PAYMENT);
        assertThatThrownBy(() -> service.issueForConfirmedBooking(booking))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Complete payment before viewing the ticket");
        verify(notificationService, never()).ticketQrGenerated(any(), any());
    }

    @Test
    void ownerCanRetrieveSafeTicketResponse() {
        Ticket ticket = ticket(booking);
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setPassenger(passenger);
        payment.setPaymentMethod(PaymentMethod.WALLET);
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setAmount(booking.getTotalFare());
        payment.setTransactionReference("PAY-20260718-ABC12345");
        payment.setPaidAt(LocalDateTime.now());
        when(userRepository.findByEmailIgnoreCase(passenger.getEmail())).thenReturn(Optional.of(passenger));
        when(ticketRepository.findByTicketNumberAndBookingPassengerEmailIgnoreCase(ticket.getTicketNumber(), passenger.getEmail()))
                .thenReturn(Optional.of(ticket));
        when(paymentRepository.findFirstByBookingAndStatusOrderByCreatedAtDesc(booking, PaymentStatus.SUCCESS))
                .thenReturn(Optional.of(payment));

        TicketResponse response = service.getByTicketNumber(passenger.getEmail(), ticket.getTicketNumber());

        assertThat(response.ticketNumber()).isEqualTo(ticket.getTicketNumber());
        assertThat(response.bookingReference()).isEqualTo(booking.getBookingReference());
        assertThat(response.passengerName()).isEqualTo(booking.getPassengerName());
        assertThat(response.qrPayload()).contains(ticket.getTicketNumber());
        assertThat(response.qrPayload()).doesNotContain("encoded");
        assertThat(response.qrPayload()).doesNotContain("wallet");
    }

    @Test
    void automaticEmailUsesPassengerEmailAndPdfAttachment() throws Exception {
        Ticket ticket = ticket(booking);
        Payment payment = successfulPayment();
        byte[] pdf = new byte[] {1, 2, 3};
        when(ticketRepository.findByTicketNumber(ticket.getTicketNumber())).thenReturn(Optional.of(ticket));
        when(paymentRepository.findFirstByBookingAndStatusOrderByCreatedAtDesc(booking, PaymentStatus.SUCCESS))
                .thenReturn(Optional.of(payment));
        when(ticketPdfService.generatePassengerTripTicketPdf(any(TicketResponse.class))).thenReturn(pdf);

        service.sendAutomaticEmail(ticket.getTicketNumber());

        verify(emailService).sendPassengerTripTicketEmail(eq(passenger.getEmail()), any(TicketResponse.class), eq(pdf));
        verify(ticketRepository).save(ticket);
        assertThat(ticket.getAutoEmailStatus()).isEqualTo(TicketEmailStatus.SENT);
        assertThat(ticket.getAutoEmailSentAt()).isNotNull();
        assertThat(ticket.getLastEmailAttemptAt()).isNotNull();
    }

    @Test
    void automaticEmailIsDeferredUntilTransactionCommit() throws Exception {
        Ticket ticket = ticket(booking);
        Payment payment = successfulPayment();
        byte[] pdf = new byte[] {1, 2, 3};
        when(ticketRepository.findByTicketNumber(ticket.getTicketNumber())).thenReturn(Optional.of(ticket));
        when(paymentRepository.findFirstByBookingAndStatusOrderByCreatedAtDesc(booking, PaymentStatus.SUCCESS))
                .thenReturn(Optional.of(payment));
        when(ticketPdfService.generatePassengerTripTicketPdf(any(TicketResponse.class))).thenReturn(pdf);

        TransactionSynchronizationManager.initSynchronization();
        try {
            service.scheduleAutomaticEmailAfterCommit(ticket.getTicketNumber());

            verify(emailService, never()).sendPassengerTripTicketEmail(anyString(), any(), any());

            for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
                synchronization.afterCommit();
            }
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        verify(emailService).sendPassengerTripTicketEmail(eq(passenger.getEmail()), any(TicketResponse.class), eq(pdf));
        assertThat(ticket.getAutoEmailStatus()).isEqualTo(TicketEmailStatus.SENT);
    }

    @Test
    void automaticEmailFailureDoesNotThrowAndMarksTicketFailed() throws Exception {
        Ticket ticket = ticket(booking);
        Payment payment = successfulPayment();
        byte[] pdf = new byte[] {1, 2, 3};
        when(ticketRepository.findByTicketNumber(ticket.getTicketNumber())).thenReturn(Optional.of(ticket));
        when(paymentRepository.findFirstByBookingAndStatusOrderByCreatedAtDesc(booking, PaymentStatus.SUCCESS))
                .thenReturn(Optional.of(payment));
        when(ticketPdfService.generatePassengerTripTicketPdf(any(TicketResponse.class))).thenReturn(pdf);
        doThrow(new MessagingException("smtp down")).when(emailService)
                .sendPassengerTripTicketEmail(anyString(), any(TicketResponse.class), any());

        service.sendAutomaticEmail(ticket.getTicketNumber());

        verify(ticketRepository).save(ticket);
        assertThat(ticket.getAutoEmailStatus()).isEqualTo(TicketEmailStatus.FAILED);
        assertThat(ticket.getAutoEmailSentAt()).isNull();
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.VALID);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void automaticEmailAlreadySentIsNotRepeated() throws Exception {
        Ticket ticket = ticket(booking);
        ticket.setAutoEmailStatus(TicketEmailStatus.SENT);
        when(ticketRepository.findByTicketNumber(ticket.getTicketNumber())).thenReturn(Optional.of(ticket));

        service.sendAutomaticEmail(ticket.getTicketNumber());

        verify(emailService, never()).sendPassengerTripTicketEmail(anyString(), any(), any());
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void manualResendStillWorksAfterAutomaticEmailWasSent() throws Exception {
        Ticket ticket = ticket(booking);
        ticket.setAutoEmailStatus(TicketEmailStatus.SENT);
        byte[] pdf = new byte[] {4, 5, 6};
        when(userRepository.findByEmailIgnoreCase(passenger.getEmail())).thenReturn(Optional.of(passenger));
        when(ticketRepository.findByTicketNumberAndBookingPassengerEmailIgnoreCase(ticket.getTicketNumber(), passenger.getEmail()))
                .thenReturn(Optional.of(ticket));
        when(paymentRepository.findFirstByBookingAndStatusOrderByCreatedAtDesc(booking, PaymentStatus.SUCCESS))
                .thenReturn(Optional.of(successfulPayment()));
        when(ticketPdfService.generatePassengerTripTicketPdf(any(TicketResponse.class))).thenReturn(pdf);

        service.sendEmail(passenger.getEmail(), ticket.getTicketNumber());

        verify(emailService).sendPassengerTripTicketEmail(eq(passenger.getEmail()), any(TicketResponse.class), eq(pdf));
    }

    @Test
    void manualResendStillWorksAfterAutomaticEmailFailure() throws Exception {
        Ticket ticket = ticket(booking);
        ticket.setAutoEmailStatus(TicketEmailStatus.FAILED);
        byte[] pdf = new byte[] {7, 8, 9};
        when(userRepository.findByEmailIgnoreCase(passenger.getEmail())).thenReturn(Optional.of(passenger));
        when(ticketRepository.findByTicketNumberAndBookingPassengerEmailIgnoreCase(ticket.getTicketNumber(), passenger.getEmail()))
                .thenReturn(Optional.of(ticket));
        when(paymentRepository.findFirstByBookingAndStatusOrderByCreatedAtDesc(booking, PaymentStatus.SUCCESS))
                .thenReturn(Optional.of(successfulPayment()));
        when(ticketPdfService.generatePassengerTripTicketPdf(any(TicketResponse.class))).thenReturn(pdf);

        service.sendEmail(passenger.getEmail(), ticket.getTicketNumber());

        verify(emailService).sendPassengerTripTicketEmail(eq(passenger.getEmail()), any(TicketResponse.class), eq(pdf));
    }

    @Test
    void anotherPassengerCannotTriggerManualResend() throws Exception {
        User otherPassenger = new User("Passenger B", "b@example.com", "9800000002", "encoded", "PASSENGER");
        when(userRepository.findByEmailIgnoreCase(otherPassenger.getEmail())).thenReturn(Optional.of(otherPassenger));
        when(ticketRepository.findByTicketNumberAndBookingPassengerEmailIgnoreCase("YT-TKT-20260718-ABC123", otherPassenger.getEmail()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.sendEmail(otherPassenger.getEmail(), "YT-TKT-20260718-ABC123"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Ticket not found");

        verify(emailService, never()).sendPassengerTripTicketEmail(anyString(), any(), any());
    }

    @Test
    void nonPassengerRolesCannotTriggerManualResend() throws Exception {
        for (String role : List.of("DRIVER", "OPERATOR", "ADMIN")) {
            User nonPassenger = new User(role, role.toLowerCase() + "@example.com", "", "", role);
            when(userRepository.findByEmailIgnoreCase(nonPassenger.getEmail())).thenReturn(Optional.of(nonPassenger));

            assertThatThrownBy(() -> service.sendEmail(nonPassenger.getEmail(), "YT-TKT-20260718-ABC123"))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Passenger access is required");
        }

        verify(emailService, never()).sendPassengerTripTicketEmail(anyString(), any(), any());
    }

    private Payment successfulPayment() {
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setPassenger(passenger);
        payment.setPaymentMethod(PaymentMethod.WALLET);
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setAmount(booking.getTotalFare());
        payment.setTransactionReference("PAY-20260718-ABC12345");
        payment.setPaidAt(LocalDateTime.now());
        return payment;
    }

    private Ticket ticket(PassengerTripBooking owner) {
        Ticket ticket = new Ticket();
        ticket.setTicketNumber("YT-TKT-20260718-ABC123");
        ticket.setBooking(owner);
        ticket.setStatus(TicketStatus.VALID);
        ticket.setQrTokenHash("a".repeat(64));
        ticket.setIssuedAt(LocalDateTime.now());
        ticket.setValidFrom(LocalDateTime.now());
        ticket.setValidUntil(owner.getScheduledTrip().getEstimatedArrivalAt().plusHours(2));
        return ticket;
    }

    private PassengerTripBooking confirmedBooking() {
        LocalDateTime departure = LocalDateTime.now().plusDays(3);
        com.yatayat.backend.entity.Route route = new com.yatayat.backend.entity.Route();
        route.setId(20L); route.setCode("KTM-PKR"); route.setName("Kathmandu to Pokhara");
        route.setOrigin("Kathmandu"); route.setDestination("Pokhara"); route.setStatus(RouteStatus.ACTIVE);
        route.setTripType(TripType.OUT_OF_VALLEY);
        TransportOperator operator = new TransportOperator(); operator.setId(30L);
        operator.setName("Safe Travels"); operator.setVerificationStatus(OperatorVerificationStatus.APPROVED);
        Bus bus = new Bus(); bus.setId(40L); bus.setBusName("Deluxe Express"); bus.setBusNumber("BA-1-KHA-1000");
        DriverProfile driver = new DriverProfile(new User("Driver", "d@example.com", "", "", "DRIVER"));
        ScheduledTrip trip = new ScheduledTrip(); trip.setId(10L); trip.setRoute(route); trip.setOperator(operator);
        trip.setBus(bus); trip.setDriver(driver); trip.setDepartureAt(departure);
        trip.setEstimatedArrivalAt(departure.plusHours(6)); trip.setFare(new BigDecimal("500.00"));
        PassengerTripBooking result = new PassengerTripBooking();
        result.setId(50L); result.setBookingReference("YAT-20260718-ABC123"); result.setPassenger(passenger);
        result.setScheduledTrip(trip); result.setPassengerName(passenger.getFullName());
        result.setPassengerPhone(passenger.getPhone()); result.setNumberOfSeats(2);
        result.setFarePerSeat(trip.getFare()); result.setTotalFare(new BigDecimal("1000.00"));
        result.setStatus(BookingStatus.CONFIRMED); result.setBookedAt(LocalDateTime.now());
        BookingSeat seatA = seat(result, "1A"); BookingSeat seatB = seat(result, "1B");
        result.setSeats(List.of(seatA, seatB));
        return result;
    }

    private BookingSeat seat(PassengerTripBooking owner, String number) {
        BookingSeat seat = new BookingSeat();
        seat.setBooking(owner); seat.setScheduledTrip(owner.getScheduledTrip());
        seat.setPassenger(owner.getPassenger()); seat.setSeatNumber(number);
        seat.setActiveSeatNumber(number); seat.setStatus(BookingSeatStatus.CONFIRMED);
        seat.setHeldAt(LocalDateTime.now()); seat.setHoldExpiresAt(LocalDateTime.now());
        return seat;
    }
}
