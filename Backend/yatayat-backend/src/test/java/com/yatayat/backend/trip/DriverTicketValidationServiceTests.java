package com.yatayat.backend.trip;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yatayat.backend.dto.DriverTicketValidationResponse;
import com.yatayat.backend.dto.DriverTripManifestResponse;
import com.yatayat.backend.entity.*;
import com.yatayat.backend.repository.*;
import com.yatayat.backend.service.DriverTicketValidationService;
import com.yatayat.backend.service.NotificationService;
import com.yatayat.backend.service.TicketQrTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DriverTicketValidationServiceTests {
    @Mock private UserRepository userRepository;
    @Mock private DriverProfileRepository driverProfileRepository;
    @Mock private DriverOperatorAssociationRepository associationRepository;
    @Mock private TicketRepository ticketRepository;
    @Mock private ScheduledTripRepository scheduledTripRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private NotificationService notificationService;

    private DriverTicketValidationService service;
    private User driverUser;
    private DriverProfile driver;
    private DriverOperatorAssociation association;
    private Ticket ticket;
    private PassengerTripBooking booking;
    private ScheduledTrip trip;
    private TicketQrTokenService qrTokens;

    @BeforeEach
    void setUp() {
        qrTokens = new TicketQrTokenService("test-only-ticket-qr-secret-at-least-32-characters");
        service = new DriverTicketValidationService(new ObjectMapper(), userRepository,
                driverProfileRepository, associationRepository, ticketRepository,
                scheduledTripRepository, paymentRepository, qrTokens, notificationService);
        driverUser = new User("Driver A", "driver@example.com", "9800000001", "encoded", "DRIVER");
        driverUser.setId(1L);
        driver = new DriverProfile(driverUser);
        driver.setId(10L);
        driver.setVerificationStatus(DriverVerificationStatus.APPROVED);
        driver.setLicenseNumber("LIC-001");
        driver.setLicenseCategory("A");
        driver.setCitizenshipNumber("CIT-001");
        driver.setLicenseExpiryDate(LocalDate.now().plusYears(1));
        trip = trip(driver);
        association = new DriverOperatorAssociation();
        association.setDriver(driver);
        association.setOperator(trip.getOperator());
        association.setStatus(DriverOperatorAssociationStatus.ACTIVE);
        booking = booking(trip);
        ticket = ticket(booking);
    }

    @Test
    void validQrBoardsTicketAtomically() {
        mockApprovedDriver();
        mockTicketPaymentAndAssociation(ticket);

        DriverTicketValidationResponse response = service.validate(driverUser.getEmail(), validQr());

        assertThat(response.result()).isEqualTo("VALID");
        assertThat(response.ticketNumber()).isEqualTo(ticket.getTicketNumber());
        assertThat(response.seatNumbers()).containsExactly("1A", "1B");
        assertThat(response.scheduledTripReference()).isEqualTo("TRIP-50");
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.USED);
        assertThat(ticket.getUsedAt()).isNotNull();
        assertThat(ticket.getValidatedByDriverProfile()).isEqualTo(driver);
        assertThat(ticket.getValidatedTrip()).isEqualTo(trip);
        verify(ticketRepository).findByTicketNumberForValidation(ticket.getTicketNumber());
        verify(ticketRepository).save(ticket);
        verify(notificationService).ticketUsed(ticket);
    }

    @Test
    void validNewSecureRawTokenIsHashedBeforeComparison() {
        mockApprovedDriver();
        ticket.setQrTokenHash(qrTokens.storedHash(ticket.getTicketNumber()));
        mockTicketPaymentAndAssociation(ticket);
        String payload = "{\"version\":1,\"ticketNumber\":\"" + ticket.getTicketNumber()
                + "\",\"token\":\"" + qrTokens.rawToken(ticket.getTicketNumber()) + "\"}";

        DriverTicketValidationResponse response =
                service.validate(driverUser.getEmail(), payload);

        assertThat(response.result()).isEqualTo("VALID");
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.USED);
    }

    @Test
    void legacyStoredHashQrRemainsCompatible() {
        mockApprovedDriver();
        mockTicketPaymentAndAssociation(ticket);

        assertThat(service.validate(driverUser.getEmail(), validQr()).result())
                .isEqualTo("VALID");
    }

    @Test
    void duplicateScanReturnsAlreadyUsedAndDoesNotChangeUsedAt() {
        mockApprovedDriver();
        when(associationRepository.findByDriverAndStatus(driver, DriverOperatorAssociationStatus.ACTIVE))
                .thenReturn(Optional.of(association));
        when(ticketRepository.findByTicketNumberForValidation(ticket.getTicketNumber()))
                .thenReturn(Optional.of(ticket));
        when(associationRepository.findByDriverAndOperator(driver, trip.getOperator()))
                .thenReturn(Optional.of(association));
        LocalDateTime usedAt = LocalDateTime.now().minusMinutes(5);
        DriverProfile originalDriver = driver;
        ScheduledTrip originalTrip = trip;
        ticket.setStatus(TicketStatus.USED);
        ticket.setUsedAt(usedAt);
        ticket.setValidatedByDriverProfile(originalDriver);
        ticket.setValidatedTrip(originalTrip);

        assertThatThrownBy(() -> service.validate(driverUser.getEmail(), validQr()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("ALREADY_USED");

        assertThat(ticket.getUsedAt()).isEqualTo(usedAt);
        assertThat(ticket.getValidatedByDriverProfile()).isSameAs(originalDriver);
        assertThat(ticket.getValidatedTrip()).isSameAs(originalTrip);
        verify(ticketRepository, never()).save(ticket);
        verify(notificationService, never()).ticketUsed(any());
    }

    @Test
    void malformedAndAlteredQrAreRejectedWithoutExposingToken() {
        mockApprovedDriver();
        when(associationRepository.findByDriverAndStatus(driver, DriverOperatorAssociationStatus.ACTIVE))
                .thenReturn(Optional.of(association));

        assertThatThrownBy(() -> service.validate(driverUser.getEmail(), "not-json"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("INVALID_QR");

        when(ticketRepository.findByTicketNumberForValidation(ticket.getTicketNumber()))
                .thenReturn(Optional.of(ticket));
        assertThatThrownBy(() -> service.validate(driverUser.getEmail(),
                "{\"version\":1,\"ticketNumber\":\"YT-TKT-20260718-ABC123\",\"token\":\"bad-token\"}"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("INVALID_QR")
                .hasMessageNotContaining(ticket.getQrTokenHash());
    }

    @Test
    void unapprovedDriverAndMissingAssociationAreDenied() {
        driver.setVerificationStatus(DriverVerificationStatus.PENDING);
        when(userRepository.findByEmailIgnoreCase(driverUser.getEmail())).thenReturn(Optional.of(driverUser));
        when(driverProfileRepository.findByUser(driverUser)).thenReturn(Optional.of(driver));

        assertThatThrownBy(() -> service.validate(driverUser.getEmail(), validQr()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Approved active driver profile");

        driver.setVerificationStatus(DriverVerificationStatus.APPROVED);
        when(associationRepository.findByDriverAndStatus(driver, DriverOperatorAssociationStatus.ACTIVE))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.validate(driverUser.getEmail(), validQr()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Active operator association");
    }

    @Test
    void unrelatedAssignedDriverCannotValidateTicket() {
        mockApprovedDriver();
        when(associationRepository.findByDriverAndStatus(driver, DriverOperatorAssociationStatus.ACTIVE))
                .thenReturn(Optional.of(association));
        when(ticketRepository.findByTicketNumberForValidation(ticket.getTicketNumber()))
                .thenReturn(Optional.of(ticket));
        DriverProfile otherDriver = new DriverProfile(new User("Driver B", "other@example.com", "", "", "DRIVER"));
        otherDriver.setId(99L);
        trip.setDriver(otherDriver);

        assertThatThrownBy(() -> service.validate(driverUser.getEmail(), validQr()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("WRONG_TRIP");
    }

    @Test
    void invalidTicketStatesAndPaymentAreRejected() {
        mockApprovedDriver();
        mockTicketPaymentAndAssociation(ticket);

        booking.setStatus(BookingStatus.PENDING_PAYMENT);
        assertThatThrownBy(() -> service.validate(driverUser.getEmail(), validQr()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("BOOKING_NOT_CONFIRMED");

        booking.setStatus(BookingStatus.CONFIRMED);
        when(paymentRepository.existsByBookingAndStatus(booking, PaymentStatus.SUCCESS)).thenReturn(false);
        assertThatThrownBy(() -> service.validate(driverUser.getEmail(), validQr()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("PAYMENT_NOT_SUCCESSFUL");

        when(paymentRepository.existsByBookingAndStatus(
                booking, PaymentStatus.SUCCESS)).thenReturn(true);
        ticket.setValidUntil(LocalDateTime.now().minusMinutes(1));
        assertThatThrownBy(() -> service.validate(driverUser.getEmail(), validQr()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("EXPIRED");
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.EXPIRED);
        verify(ticketRepository).saveAndFlush(ticket);
    }

    @Test
    void cancelledTicketAndBookingPersistCancelledLifecycle() {
        mockApprovedDriver();
        mockTicketPaymentAndAssociation(ticket);
        ticket.setStatus(TicketStatus.CANCELLED);
        assertThatThrownBy(() -> service.validate(driverUser.getEmail(), validQr()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("CANCELLED");

        ticket.setStatus(TicketStatus.VALID);
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(LocalDateTime.now().minusMinutes(1));
        assertThatThrownBy(() -> service.validate(driverUser.getEmail(), validQr()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("CANCELLED");

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.CANCELLED);
        assertThat(ticket.getCancelledAt()).isEqualTo(booking.getCancelledAt());
        verify(ticketRepository).saveAndFlush(ticket);
    }

    @Test
    void notYetValidTicketIsRejected() {
        mockApprovedDriver();
        mockTicketPaymentAndAssociation(ticket);
        ticket.setValidFrom(LocalDateTime.now().plusMinutes(5));

        assertThatThrownBy(() -> service.validate(driverUser.getEmail(), validQr()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("NOT_YET_VALID");
        verify(ticketRepository, never()).save(ticket);
    }

    @Test
    void completedAndCancelledTripsAreNotBoardable() {
        mockApprovedDriver();
        mockTicketPaymentAndAssociation(ticket);

        for (TripStatus status : List.of(TripStatus.COMPLETED, TripStatus.CANCELLED)) {
            trip.setStatus(status);
            assertThatThrownBy(() -> service.validate(driverUser.getEmail(), validQr()))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("TRIP_NOT_BOARDABLE");
        }
    }

    @Test
    void expiredDriverLicenceIsDenied() {
        driver.setLicenseExpiryDate(LocalDate.now().minusDays(1));
        mockApprovedDriver();

        assertThatThrownBy(() -> service.validate(driverUser.getEmail(), validQr()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Approved active driver profile");
        verifyNoInteractions(ticketRepository);
    }

    @Test
    void malformedMissingUnsupportedAndOversizedPayloadsAreControlled() {
        mockApprovedDriver();
        when(associationRepository.findByDriverAndStatus(
                driver, DriverOperatorAssociationStatus.ACTIVE))
                .thenReturn(Optional.of(association));

        List<String> invalidPayloads = List.of(
                "not-json",
                "{\"version\":1,\"ticketNumber\":\"\",\"token\":\"token\"}",
                "{\"version\":1,\"ticketNumber\":\"YT-TKT-1\",\"token\":\"\"}",
                "{\"version\":2,\"ticketNumber\":\"YT-TKT-1\",\"token\":\"token\"}",
                "x".repeat(DriverTicketValidationService.MAX_QR_PAYLOAD_LENGTH + 1)
        );
        for (String payload : invalidPayloads) {
            assertThatThrownBy(() -> service.validate(driverUser.getEmail(), payload))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("INVALID_QR");
        }
        verify(ticketRepository, never()).findByTicketNumberForValidation(any());
    }

    @Test
    void assignedDriverCanViewManifestWithoutSensitiveQrData() {
        mockApprovedDriver();
        when(scheduledTripRepository.findById(50L)).thenReturn(Optional.of(trip));
        when(associationRepository.findByDriverAndOperator(driver, trip.getOperator()))
                .thenReturn(Optional.of(association));
        ticket.setStatus(TicketStatus.USED);
        ticket.setUsedAt(LocalDateTime.now());
        when(ticketRepository.findByBookingScheduledTripAndBookingStatusOrderByBookingPassengerNameAsc(
                trip, BookingStatus.CONFIRMED)).thenReturn(List.of(ticket));
        when(paymentRepository.existsByBookingAndStatus(
                booking, PaymentStatus.SUCCESS)).thenReturn(true);

        DriverTripManifestResponse manifest = service.manifest(driverUser.getEmail(), 50L);

        assertThat(manifest.trip().scheduledTripReference()).isEqualTo("TRIP-50");
        assertThat(manifest.summary().boardedPassengers()).isEqualTo(1);
        assertThat(manifest.passengers()).hasSize(1);
        assertThat(manifest.passengers().get(0).ticketNumber()).isEqualTo(ticket.getTicketNumber());
        assertThat(manifest.toString()).doesNotContain(ticket.getQrTokenHash());
    }

    private void mockApprovedDriver() {
        when(userRepository.findByEmailIgnoreCase(driverUser.getEmail())).thenReturn(Optional.of(driverUser));
        when(driverProfileRepository.findByUser(driverUser)).thenReturn(Optional.of(driver));
    }

    private void mockTicketPaymentAndAssociation(Ticket value) {
        when(associationRepository.findByDriverAndStatus(driver, DriverOperatorAssociationStatus.ACTIVE))
                .thenReturn(Optional.of(association));
        when(ticketRepository.findByTicketNumberForValidation(value.getTicketNumber()))
                .thenReturn(Optional.of(value));
        when(associationRepository.findByDriverAndOperator(driver, trip.getOperator()))
                .thenReturn(Optional.of(association));
        lenient().when(paymentRepository.existsByBookingAndStatus(
                booking, PaymentStatus.SUCCESS)).thenReturn(true);
    }

    private String validQr() {
        return "{\"version\":1,\"ticketNumber\":\"YT-TKT-20260718-ABC123\",\"token\":\"" + ticket.getQrTokenHash() + "\"}";
    }

    private ScheduledTrip trip(DriverProfile assignedDriver) {
        com.yatayat.backend.entity.Route route = new com.yatayat.backend.entity.Route();
        route.setId(20L);
        route.setCode("KTM-PKR");
        route.setName("Kathmandu to Pokhara");
        route.setOrigin("Kathmandu");
        route.setDestination("Pokhara");
        route.setStatus(RouteStatus.ACTIVE);

        TransportOperator operator = new TransportOperator();
        operator.setId(30L);
        operator.setName("Safe Travels");
        operator.setVerificationStatus(OperatorVerificationStatus.APPROVED);

        Bus bus = new Bus();
        bus.setId(40L);
        bus.setBusName("Deluxe Express");
        bus.setBusNumber("BA-1-KHA-1000");
        bus.setStatus(BusStatus.APPROVED);

        ScheduledTrip result = new ScheduledTrip();
        result.setId(50L);
        result.setRoute(route);
        result.setOperator(operator);
        result.setBus(bus);
        result.setDriver(assignedDriver);
        result.setDepartureAt(LocalDateTime.now().plusHours(1));
        result.setEstimatedArrivalAt(LocalDateTime.now().plusHours(7));
        result.setFare(new BigDecimal("500.00"));
        result.setSeatCapacitySnapshot(40);
        result.setStatus(TripStatus.BOARDING);
        return result;
    }

    private PassengerTripBooking booking(ScheduledTrip owner) {
        User passenger = new User("Passenger A", "passenger@example.com", "9800000002", "encoded", "PASSENGER");
        passenger.setId(2L);
        PassengerTripBooking result = new PassengerTripBooking();
        result.setId(60L);
        result.setBookingReference("YAT-20260718-ABC123");
        result.setPassenger(passenger);
        result.setScheduledTrip(owner);
        result.setPassengerName(passenger.getFullName());
        result.setPassengerPhone(passenger.getPhone());
        result.setNumberOfSeats(2);
        result.setFarePerSeat(owner.getFare());
        result.setTotalFare(new BigDecimal("1000.00"));
        result.setStatus(BookingStatus.CONFIRMED);
        result.setBookedAt(LocalDateTime.now());
        result.setSeats(List.of(seat(result, "1A"), seat(result, "1B")));
        return result;
    }

    private BookingSeat seat(PassengerTripBooking owner, String number) {
        BookingSeat seat = new BookingSeat();
        seat.setBooking(owner);
        seat.setScheduledTrip(owner.getScheduledTrip());
        seat.setPassenger(owner.getPassenger());
        seat.setSeatNumber(number);
        seat.setActiveSeatNumber(number);
        seat.setStatus(BookingSeatStatus.CONFIRMED);
        seat.setHeldAt(LocalDateTime.now());
        seat.setHoldExpiresAt(LocalDateTime.now());
        return seat;
    }

    private Ticket ticket(PassengerTripBooking owner) {
        Ticket result = new Ticket();
        result.setTicketNumber("YT-TKT-20260718-ABC123");
        result.setBooking(owner);
        result.setStatus(TicketStatus.VALID);
        result.setQrTokenHash("a".repeat(64));
        result.setIssuedAt(LocalDateTime.now());
        result.setValidFrom(LocalDateTime.now().minusMinutes(10));
        result.setValidUntil(LocalDateTime.now().plusHours(8));
        return result;
    }
}
