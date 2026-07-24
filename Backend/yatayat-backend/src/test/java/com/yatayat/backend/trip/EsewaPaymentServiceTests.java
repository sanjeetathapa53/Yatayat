package com.yatayat.backend.trip;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yatayat.backend.dto.*;
import com.yatayat.backend.entity.*;
import com.yatayat.backend.payment.*;
import com.yatayat.backend.repository.*;
import com.yatayat.backend.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EsewaPaymentServiceTests {
    private EsewaProperties properties;
    private EsewaGateway gateway;
    private EsewaSignatureService signatures;
    private UserRepository users;
    private PassengerTripBookingRepository bookings;
    private BookingSeatRepository seats;
    private PaymentRepository payments;
    private PassengerTicketService tickets;
    private EsewaPaymentService service;
    private User passenger;
    private PassengerTripBooking booking;
    private Payment payment;

    @BeforeEach
    void setUp() {
        properties = new EsewaProperties();
        properties.setEnabled(true);
        properties.setProductCode("EPAYTEST");
        properties.setSecretKey("unit-test-secret");
        gateway = mock(EsewaGateway.class);
        signatures = new EsewaSignatureService();
        users = mock(UserRepository.class);
        bookings = mock(PassengerTripBookingRepository.class);
        seats = mock(BookingSeatRepository.class);
        payments = mock(PaymentRepository.class);
        tickets = mock(PassengerTicketService.class);
        service = new EsewaPaymentService(properties, gateway, signatures, new ObjectMapper(),
                users, bookings, seats, payments, tickets);

        passenger = new User("Passenger", "passenger@example.com", "9800000000",
                "encoded", "PASSENGER");
        passenger.setId(7L);
        booking = pendingBooking();
        payment = initiatedPayment();
        when(users.findByEmailIgnoreCase(passenger.getEmail()))
                .thenReturn(Optional.of(passenger));
        when(bookings.findOwnedByReferenceForPayment(
                booking.getBookingReference(), passenger.getId()))
                .thenReturn(Optional.of(booking));
        when(seats.findWithLockByBookingOrderBySeatNumberAsc(booking))
                .thenReturn(booking.getSeats());
        when(payments.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(bookings.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(payments.existsByTransactionReference(anyString())).thenReturn(false);
    }

    @Test
    void validInitiationUsesBackendAmountAndSignedOfficialForm() {
        EsewaPaymentInitiationResponse response =
                service.initiate(passenger.getEmail(), booking.getBookingReference());

        var saved = org.mockito.ArgumentCaptor.forClass(Payment.class);
        verify(payments).saveAndFlush(saved.capture());
        Payment attempt = saved.getValue();
        assertThat(attempt.getAmount()).isEqualByComparingTo("1000.00");
        assertThat(attempt.getPaymentMethod()).isEqualTo(PaymentMethod.ESEWA);
        assertThat(attempt.getProviderPaymentId()).matches("[A-Z0-9-]+");
        assertThat(response.formAction()).isEqualTo(
                "https://rc-epay.esewa.com.np/api/epay/main/v2/form");
        assertThat(response.formFields())
                .containsEntry("amount", "1000.00")
                .containsEntry("total_amount", "1000.00")
                .containsEntry("product_code", "EPAYTEST")
                .containsEntry("transaction_uuid", attempt.getProviderPaymentId());
        assertThat(response.formFields().get("success_url"))
                .endsWith("/passenger/payments/esewa/callback/success/"
                        + booking.getBookingReference())
                .doesNotContain("transactionUuid", "?");
        assertThat(response.formFields().get("failure_url"))
                .endsWith("/passenger/payments/esewa/callback/failure/"
                        + booking.getBookingReference() + "/" + attempt.getProviderPaymentId())
                .doesNotContain("?");
        String message = "total_amount=1000.00,transaction_uuid="
                + attempt.getProviderPaymentId() + ",product_code=EPAYTEST";
        assertThat(signatures.verify(message, response.formFields().get("signature"),
                properties.getSecretKey())).isTrue();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        verifyNoInteractions(gateway, tickets);
    }

    @Test
    void duplicateInitiationReusesStoredTransactionAndCreatesNoSecondPayment() {
        when(payments.findFirstByBookingAndPaymentMethodAndStatusOrderByCreatedAtDesc(
                booking, PaymentMethod.ESEWA, PaymentStatus.INITIATED))
                .thenReturn(Optional.of(payment));

        EsewaPaymentInitiationResponse response =
                service.initiate(passenger.getEmail(), booking.getBookingReference());

        assertThat(response.formFields().get("transaction_uuid"))
                .isEqualTo(payment.getProviderPaymentId());
        verify(payments, never()).saveAndFlush(any());
        verifyNoInteractions(gateway, tickets);
    }

    @Test
    void disabledMissingConfigurationAndOwnershipAreRejected() {
        properties.setEnabled(false);
        assertStatus(503, () -> service.initiate(
                passenger.getEmail(), booking.getBookingReference()));
        properties.setEnabled(true);
        User other = new User("Other", "other@example.com", "", "", "PASSENGER");
        other.setId(8L);
        when(users.findByEmailIgnoreCase(other.getEmail())).thenReturn(Optional.of(other));
        assertStatus(404, () -> service.initiate(
                other.getEmail(), booking.getBookingReference()));
        properties.setProductCode("");
        assertThatThrownBy(properties::validate).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void completeServerStatusConfirmsAndRepeatedCallbackIsIdempotent() {
        prepareVerification();
        when(gateway.lookup("EPAYTEST", "1000.00", payment.getProviderPaymentId()))
                .thenReturn(status("COMPLETE", "0001TEST", "1000.00",
                        "EPAYTEST", payment.getProviderPaymentId()));
        Ticket ticket = ticket();
        when(tickets.issueForConfirmedBooking(booking)).thenReturn(ticket);
        EsewaPaymentVerificationRequest request =
                new EsewaPaymentVerificationRequest(payment.getProviderPaymentId(),
                        signedCallback(payment.getProviderPaymentId(), "EPAYTEST", "1000.00"));

        ExternalPaymentVerificationResponse first = service.verify(
                passenger.getEmail(), booking.getBookingReference(), request);
        ExternalPaymentVerificationResponse repeated = service.verify(
                passenger.getEmail(), booking.getBookingReference(), request);

        assertThat(first.bookingStatus()).isEqualTo("CONFIRMED");
        assertThat(first.paymentStatus()).isEqualTo("SUCCESS");
        assertThat(first.ticketNumber()).isEqualTo(ticket.getTicketNumber());
        assertThat(repeated.ticketNumber()).isEqualTo(ticket.getTicketNumber());
        verify(gateway, times(1)).lookup("EPAYTEST", "1000.00",
                payment.getProviderPaymentId());
        verify(tickets, times(1))
                .scheduleAutomaticEmailAfterCommit(ticket.getTicketNumber());
    }

    @Test
    void signedCallbackUuidIsAuthoritativeWhenUnsignedUuidContainsAppendedData() {
        prepareVerification();
        String signedData = signedCallback(
                payment.getProviderPaymentId(), "EPAYTEST", "1000.00");
        String corruptedUnsigned = payment.getProviderPaymentId() + "?data=" + signedData;
        when(gateway.lookup("EPAYTEST", "1000.00", payment.getProviderPaymentId()))
                .thenReturn(status("COMPLETE", "0001TEST", "1000.00",
                        "EPAYTEST", payment.getProviderPaymentId()));
        when(tickets.issueForConfirmedBooking(booking)).thenReturn(ticket());

        ExternalPaymentVerificationResponse response = service.verify(
                passenger.getEmail(), booking.getBookingReference(),
                new EsewaPaymentVerificationRequest(corruptedUnsigned, signedData));

        assertThat(response.paymentStatus()).isEqualTo("SUCCESS");
        verify(payments).findByBookingAndPaymentMethodAndProviderPaymentId(
                booking, PaymentMethod.ESEWA, payment.getProviderPaymentId());
        verify(payments, never()).findByBookingAndPaymentMethodAndProviderPaymentId(
                booking, PaymentMethod.ESEWA, corruptedUnsigned);
    }

    @Test
    void invalidCallbackSignatureCannotReachServerVerification() {
        prepareVerification();
        String callback = callbackData(payment.getProviderPaymentId(),
                "EPAYTEST", "1000.00", "invalid-signature");
        assertStatus(409, () -> service.verify(passenger.getEmail(),
                booking.getBookingReference(), new EsewaPaymentVerificationRequest(
                        payment.getProviderPaymentId(), callback)));
        verifyNoInteractions(gateway, tickets);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
    }

    @Test
    void callbackAmountProductAndTransactionTamperingAreRejected() {
        prepareVerification();
        assertStatus(409, () -> service.verify(passenger.getEmail(),
                booking.getBookingReference(), new EsewaPaymentVerificationRequest(
                        payment.getProviderPaymentId(),
                        signedCallback(payment.getProviderPaymentId(), "EPAYTEST", "999.00"))));
        assertStatus(409, () -> service.verify(passenger.getEmail(),
                booking.getBookingReference(), new EsewaPaymentVerificationRequest(
                        payment.getProviderPaymentId(),
                        signedCallback(payment.getProviderPaymentId(), "OTHER", "1000.00"))));
        when(payments.findByBookingAndPaymentMethodAndProviderPaymentId(
                booking, PaymentMethod.ESEWA, "OTHER-UUID")).thenReturn(Optional.empty());
        assertStatus(404, () -> service.verify(passenger.getEmail(),
                booking.getBookingReference(), new EsewaPaymentVerificationRequest(
                        payment.getProviderPaymentId(),
                        signedCallback("OTHER-UUID", "EPAYTEST", "1000.00"))));
        verifyNoInteractions(gateway, tickets);
    }

    @Test
    void serverStatusIdentityAndAmountMismatchesCannotConfirm() {
        prepareVerification();
        when(gateway.lookup(anyString(), anyString(), anyString()))
                .thenReturn(status("COMPLETE", "ref", "999.00",
                        "EPAYTEST", payment.getProviderPaymentId()));
        assertStatus(409, this::verifyPayment);
        when(gateway.lookup(anyString(), anyString(), anyString()))
                .thenReturn(status("COMPLETE", "ref", "1000.00",
                        "OTHER", payment.getProviderPaymentId()));
        assertStatus(409, this::verifyPayment);
        when(gateway.lookup(anyString(), anyString(), anyString()))
                .thenReturn(status("COMPLETE", "ref", "1000.00",
                        "EPAYTEST", "OTHER-UUID"));
        assertStatus(409, this::verifyPayment);
        verifyNoInteractions(tickets);
    }

    @Test
    void completeWithoutReferenceIdCannotConfirmOrIssueTicket() {
        prepareVerification();
        when(gateway.lookup(anyString(), anyString(), anyString()))
                .thenReturn(status("COMPLETE", "", "1000.00",
                        "EPAYTEST", payment.getProviderPaymentId()));
        assertStatus(409, () -> service.verify(passenger.getEmail(),
                booking.getBookingReference(), new EsewaPaymentVerificationRequest(
                        payment.getProviderPaymentId(),
                        signedCallback(payment.getProviderPaymentId(),
                                "EPAYTEST", "1000.00"))));
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        verifyNoInteractions(tickets);
    }

    @Test
    void missingCallbackDataCannotConfirmEvenWhenStatusLookupIsComplete() {
        prepareVerification();
        when(gateway.lookup("EPAYTEST", "1000.00", payment.getProviderPaymentId()))
                .thenReturn(status("COMPLETE", "0001TEST", "1000.00",
                        "EPAYTEST", payment.getProviderPaymentId()));

        assertStatus(409, this::verifyPayment);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.INITIATED);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        verifyNoInteractions(tickets);
    }

    @Test
    void nonCompleteStatusesMapWithoutTicketOrBookingConfirmation() {
        Map<String, PaymentStatus> mappings = new LinkedHashMap<>();
        mappings.put("PENDING", PaymentStatus.PENDING);
        mappings.put("FAILED", PaymentStatus.FAILED);
        mappings.put("CANCELED", PaymentStatus.CANCELLED);
        mappings.put("FULL_REFUND", PaymentStatus.REFUNDED);
        mappings.put("NOT_FOUND", PaymentStatus.EXPIRED);
        mappings.put("EXPIRED", PaymentStatus.EXPIRED);

        for (var entry : mappings.entrySet()) {
            payment.setStatus(PaymentStatus.INITIATED);
            prepareVerification();
            when(gateway.lookup(anyString(), anyString(), anyString()))
                    .thenReturn(status(entry.getKey(), null, "1000.00",
                            "EPAYTEST", payment.getProviderPaymentId()));
            ExternalPaymentVerificationResponse response = verifyPayment();
            assertThat(response.paymentStatus()).isEqualTo(entry.getValue().name());
            assertThat(response.bookingStatus()).isEqualTo("PENDING_PAYMENT");
            assertThat(response.ticketNumber()).isNull();
        }
        verifyNoInteractions(tickets);
    }

    @Test
    void unknownTransactionAndWrongOwnerAreConcealed() {
        when(payments.findByBookingAndPaymentMethodAndProviderPaymentId(
                booking, PaymentMethod.ESEWA, "UNKNOWN")).thenReturn(Optional.empty());
        assertStatus(404, () -> service.verify(passenger.getEmail(),
                booking.getBookingReference(),
                new EsewaPaymentVerificationRequest("UNKNOWN", "")));

        User other = new User("Other", "other@example.com", "", "", "PASSENGER");
        other.setId(8L);
        when(users.findByEmailIgnoreCase(other.getEmail())).thenReturn(Optional.of(other));
        assertStatus(404, () -> service.verify(other.getEmail(),
                booking.getBookingReference(),
                new EsewaPaymentVerificationRequest(payment.getProviderPaymentId(), "")));
        verifyNoInteractions(gateway, tickets);
    }

    private void prepareVerification() {
        when(payments.findByBookingAndPaymentMethodAndProviderPaymentId(
                booking, PaymentMethod.ESEWA, payment.getProviderPaymentId()))
                .thenReturn(Optional.of(payment));
    }

    private ExternalPaymentVerificationResponse verifyPayment() {
        return service.verify(passenger.getEmail(), booking.getBookingReference(),
                new EsewaPaymentVerificationRequest(payment.getProviderPaymentId(), ""));
    }

    private EsewaGateway.StatusResult status(
            String status, String reference, String amount,
            String productCode, String transactionUuid) {
        return new EsewaGateway.StatusResult(productCode, transactionUuid,
                new BigDecimal(amount), status, reference);
    }

    private String signedCallback(String uuid, String productCode, String amount) {
        String signedNames = "transaction_code,status,total_amount,transaction_uuid,"
                + "product_code,signed_field_names";
        String canonical = "transaction_code=0001TEST,status=COMPLETE,total_amount="
                + amount + ",transaction_uuid=" + uuid + ",product_code=" + productCode
                + ",signed_field_names=" + signedNames;
        return callbackData(uuid, productCode, amount,
                signatures.sign(canonical, properties.getSecretKey()));
    }

    private String callbackData(String uuid, String productCode,
                                String amount, String signature) {
        String signedNames = "transaction_code,status,total_amount,transaction_uuid,"
                + "product_code,signed_field_names";
        String json = "{\"transaction_code\":\"0001TEST\",\"status\":\"COMPLETE\","
                + "\"total_amount\":\"" + amount + "\",\"transaction_uuid\":\"" + uuid
                + "\",\"product_code\":\"" + productCode
                + "\",\"signed_field_names\":\"" + signedNames
                + "\",\"signature\":\"" + signature + "\"}";
        return Base64.getEncoder().encodeToString(
                json.getBytes(StandardCharsets.UTF_8));
    }

    private PassengerTripBooking pendingBooking() {
        Route route = new Route();
        route.setTripType(TripType.OUT_OF_VALLEY);
        ScheduledTrip trip = new ScheduledTrip();
        trip.setId(10L);
        trip.setRoute(route);
        trip.setDepartureAt(LocalDateTime.now().plusDays(1));
        PassengerTripBooking result = new PassengerTripBooking();
        result.setId(20L);
        result.setBookingReference("YAT-20260723-ESEWA");
        result.setPassenger(passenger);
        result.setScheduledTrip(trip);
        result.setPassengerName("Passenger");
        result.setPassengerPhone("9800000000");
        result.setNumberOfSeats(2);
        result.setFarePerSeat(new BigDecimal("500.00"));
        result.setTotalFare(new BigDecimal("1000.00"));
        result.setStatus(BookingStatus.PENDING_PAYMENT);
        BookingSeat first = heldSeat(result, "1A");
        BookingSeat second = heldSeat(result, "1B");
        result.setSeats(List.of(first, second));
        return result;
    }

    private BookingSeat heldSeat(PassengerTripBooking owner, String number) {
        BookingSeat seat = new BookingSeat();
        seat.setBooking(owner);
        seat.setScheduledTrip(owner.getScheduledTrip());
        seat.setPassenger(passenger);
        seat.setSeatNumber(number);
        seat.setActiveSeatNumber(number);
        seat.setStatus(BookingSeatStatus.HELD);
        seat.setHeldAt(LocalDateTime.now());
        seat.setHoldExpiresAt(LocalDateTime.now().plusMinutes(5));
        return seat;
    }

    private Payment initiatedPayment() {
        Payment result = new Payment();
        result.setId(30L);
        result.setBooking(booking);
        result.setPassenger(passenger);
        result.setAmount(booking.getTotalFare());
        result.setPaymentMethod(PaymentMethod.ESEWA);
        result.setStatus(PaymentStatus.INITIATED);
        result.setTransactionReference("ESEWA-TEST-001");
        result.setProviderPaymentId("20260723-TESTUUID");
        result.setInitiatedAt(LocalDateTime.now());
        return result;
    }

    private Ticket ticket() {
        Ticket result = new Ticket();
        result.setTicketNumber("YT-TKT-ESEWA-001");
        return result;
    }

    private void assertStatus(int status, ThrowingCallable callable) {
        assertThatThrownBy(callable::call)
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode().value()).isEqualTo(status));
    }

    @FunctionalInterface
    private interface ThrowingCallable { Object call(); }
}
