package com.yatayat.backend.service;

import com.yatayat.backend.dto.*;
import com.yatayat.backend.entity.*;
import com.yatayat.backend.payment.*;
import com.yatayat.backend.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.*;
import java.net.URI;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

@Service
public class KhaltiPaymentService {
    private final KhaltiProperties properties;
    private final KhaltiGateway gateway;
    private final UserRepository userRepository;
    private final PassengerTripBookingRepository bookingRepository;
    private final BookingSeatRepository seatRepository;
    private final PaymentRepository paymentRepository;
    private final PassengerTicketService ticketService;

    public KhaltiPaymentService(KhaltiProperties properties, KhaltiGateway gateway,
                                UserRepository userRepository,
                                PassengerTripBookingRepository bookingRepository,
                                BookingSeatRepository seatRepository,
                                PaymentRepository paymentRepository,
                                PassengerTicketService ticketService) {
        this.properties = properties;
        this.gateway = gateway;
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
        this.seatRepository = seatRepository;
        this.paymentRepository = paymentRepository;
        this.ticketService = ticketService;
    }

    @Transactional(dontRollbackOn = BookingExpiredException.class)
    public ExternalPaymentInitiationResponse initiate(String email, String reference) {
        requireConfigured();
        User passenger = requirePassenger(email);
        PassengerTripBooking booking = ownedLockedBooking(reference, passenger);
        Payment successful = paymentRepository
                .findFirstByBookingAndStatusOrderByCreatedAtDesc(booking, PaymentStatus.SUCCESS)
                .orElse(null);
        if (booking.getStatus() == BookingStatus.CONFIRMED && successful != null) {
            return initiationResponse(successful, booking, null,
                    "This booking has already been paid.");
        }
        rejectClosedBooking(booking);
        List<BookingSeat> seats = ensureSeatsPayable(booking);

        Payment existing = paymentRepository
                .findFirstByBookingAndPaymentMethodAndStatusOrderByCreatedAtDesc(
                        booking, PaymentMethod.KHALTI, PaymentStatus.INITIATED)
                .orElse(null);
        LocalDateTime now = LocalDateTime.now();
        if (existing != null && notBlank(existing.getProviderPaymentId())
                && validPaymentUrl(existing.getProviderPaymentUrl())
                && existing.getProviderExpiresAt() != null
                && existing.getProviderExpiresAt().isAfter(now)) {
            return initiationResponse(existing, booking, existing.getProviderPaymentUrl(),
                    "Continue to Khalti sandbox checkout.");
        }
        if (existing != null) {
            existing.setStatus(PaymentStatus.EXPIRED);
            existing.setFailureReason("Khalti payment initiation expired before verification.");
            paymentRepository.saveAndFlush(existing);
        }

        long paisa = toPaisa(booking.getTotalFare());
        String returnUrl = UriComponentsBuilder.fromUriString(properties.getFrontendBaseUrl())
                .path("/passenger/payments/khalti/callback")
                .queryParam("bookingReference", booking.getBookingReference())
                .build().encode().toUriString();
        KhaltiGateway.InitiationResult result = gateway.initiate(new KhaltiGateway.InitiationRequest(
                returnUrl, properties.getFrontendBaseUrl(), paisa,
                booking.getBookingReference(), "Yatayat Out-of-Valley Booking",
                new KhaltiGateway.CustomerInfo(booking.getPassengerName(),
                        passenger.getEmail(), booking.getPassengerPhone())));
        validateInitiation(result);

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setPassenger(passenger);
        payment.setAmount(booking.getTotalFare());
        payment.setPaymentMethod(PaymentMethod.KHALTI);
        payment.setStatus(PaymentStatus.INITIATED);
        payment.setTransactionReference("KHALTI-"
                + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase());
        payment.setInitiatedAt(now);
        payment.setProviderPaymentId(result.pidx());
        payment.setProviderPaymentUrl(result.paymentUrl());
        payment.setProviderExpiresAt(providerExpiry(result, now));
        try {
            paymentRepository.saveAndFlush(payment);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Khalti payment initiation conflicts with an existing attempt.");
        }
        return initiationResponse(payment, booking, result.paymentUrl(),
                "Continue to Khalti sandbox checkout.");
    }

    @Transactional(dontRollbackOn = {BookingExpiredException.class, VerificationRejectedException.class})
    public ExternalPaymentVerificationResponse verify(String email, String reference,
                                                      KhaltiPaymentVerificationRequest request) {
        requireConfigured();
        User passenger = requirePassenger(email);
        if (request == null || !notBlank(request.pidx()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Khalti pidx is required.");
        PassengerTripBooking booking = ownedLockedBooking(reference, passenger);
        Payment payment = paymentRepository.findByBookingAndPaymentMethodAndProviderPaymentId(
                        booking, PaymentMethod.KHALTI, request.pidx().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Khalti payment attempt not found."));
        if (!request.pidx().trim().equals(payment.getProviderPaymentId()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Khalti pidx does not match this booking.");
        if (payment.getStatus() == PaymentStatus.SUCCESS) return successfulResponse(payment, booking);
        rejectClosedBooking(booking);
        List<BookingSeat> seats = ensureSeatsPayable(booking);

        KhaltiGateway.LookupResult lookup = gateway.lookup(payment.getProviderPaymentId());
        validateLookupIdentity(payment, lookup);
        String providerStatus = lookup.status().trim();
        if (Boolean.TRUE.equals(lookup.refunded())) {
            return nonSuccessful(payment, booking, PaymentStatus.REFUNDED,
                    "Khalti reports this transaction as refunded.");
        }
        if (!"Completed".equals(providerStatus)) {
            return switch (providerStatus) {
                case "Pending" -> nonSuccessful(payment, booking, PaymentStatus.PENDING,
                        "Khalti payment is pending.");
                case "Initiated" -> nonSuccessful(payment, booking, PaymentStatus.INITIATED,
                        "Khalti payment is still initiated.");
                case "User canceled" -> nonSuccessful(payment, booking, PaymentStatus.CANCELLED,
                        "Khalti payment was cancelled by the user.");
                case "Expired" -> nonSuccessful(payment, booking, PaymentStatus.EXPIRED,
                        "Khalti payment expired.");
                case "Refunded" -> nonSuccessful(payment, booking, PaymentStatus.REFUNDED,
                        "Khalti payment was refunded.");
                default -> nonSuccessful(payment, booking, PaymentStatus.FAILED,
                        "Khalti payment was not completed.");
            };
        }

        long expectedPaisa = toPaisa(booking.getTotalFare());
        if (lookup.totalAmount() == null || lookup.totalAmount() != expectedPaisa)
            rejectVerification(payment, "Khalti amount does not match the booking total.");
        if (!notBlank(lookup.transactionId()))
            rejectVerification(payment, "Khalti did not return a transaction ID.");

        LocalDateTime verifiedAt = LocalDateTime.now();
        payment.setProviderTransactionId(lookup.transactionId());
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaidAt(verifiedAt);
        payment.setVerifiedAt(verifiedAt);
        payment.setFailureReason(null);
        booking.setStatus(BookingStatus.CONFIRMED);
        seats.forEach(seat -> {
            seat.setStatus(BookingSeatStatus.CONFIRMED);
            seat.setHoldExpiresAt(verifiedAt);
        });
        try {
            paymentRepository.saveAndFlush(payment);
            seatRepository.saveAll(seats);
            bookingRepository.saveAndFlush(booking);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Khalti transaction has already been processed.");
        }
        Ticket ticket = ticketService.issueForConfirmedBooking(booking);
        ticketService.scheduleAutomaticEmailAfterCommit(ticket.getTicketNumber());
        return successfulResponse(payment, booking);
    }

    public static long toPaisa(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Booking amount is invalid.");
        try {
            return amount.movePointRight(2).setScale(0, RoundingMode.UNNECESSARY).longValueExact();
        } catch (ArithmeticException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Booking amount cannot be represented exactly in paisa.");
        }
    }

    private void validateInitiation(KhaltiGateway.InitiationResult result) {
        if (result == null || !notBlank(result.pidx()) || !validPaymentUrl(result.paymentUrl()))
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Khalti returned an invalid initiation response.");
    }
    private void validateLookupIdentity(Payment payment, KhaltiGateway.LookupResult lookup) {
        if (lookup == null || !notBlank(lookup.pidx()) || !notBlank(lookup.status()))
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Khalti returned an invalid lookup response.");
        if (!payment.getProviderPaymentId().equals(lookup.pidx()))
            rejectVerification(payment, "Khalti lookup pidx does not match the stored payment.");
    }
    private boolean validPaymentUrl(String value) {
        if (!notBlank(value)) return false;
        try {
            URI uri = URI.create(value.trim());
            return "https".equalsIgnoreCase(uri.getScheme())
                    && "test-pay.khalti.com".equalsIgnoreCase(uri.getHost());
        } catch (RuntimeException exception) { return false; }
    }
    private LocalDateTime providerExpiry(KhaltiGateway.InitiationResult result, LocalDateTime now) {
        if (notBlank(result.expiresAt())) {
            try { return OffsetDateTime.parse(result.expiresAt())
                    .atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime(); }
            catch (DateTimeParseException ignored) { }
        }
        return now.plusSeconds(result.expiresIn() == null ? 1800 : Math.max(60, result.expiresIn()));
    }
    private ExternalPaymentVerificationResponse nonSuccessful(Payment payment,
                                                               PassengerTripBooking booking,
                                                               PaymentStatus status, String message) {
        payment.setStatus(status);
        payment.setFailureReason(message);
        paymentRepository.saveAndFlush(payment);
        return new ExternalPaymentVerificationResponse(booking.getBookingReference(),
                booking.getStatus().name(), "KHALTI", payment.getTransactionReference(),
                payment.getProviderPaymentId(), status.name(), null, null, null);
    }
    private void rejectVerification(Payment payment, String message) {
        payment.setFailureReason(message);
        paymentRepository.saveAndFlush(payment);
        throw new VerificationRejectedException(message);
    }
    private ExternalPaymentInitiationResponse initiationResponse(Payment payment,
                                                                 PassengerTripBooking booking,
                                                                 String redirectUrl, String message) {
        return new ExternalPaymentInitiationResponse(booking.getBookingReference(), "KHALTI",
                payment.getTransactionReference(), payment.getAmount(), payment.getStatus().name(),
                payment.getInitiatedAt(), true, redirectUrl, message);
    }
    private ExternalPaymentVerificationResponse successfulResponse(Payment payment,
                                                                    PassengerTripBooking booking) {
        Ticket ticket = ticketService.issueForConfirmedBooking(booking);
        return new ExternalPaymentVerificationResponse(booking.getBookingReference(),
                booking.getStatus().name(), "KHALTI", payment.getTransactionReference(),
                payment.getProviderTransactionId(), payment.getStatus().name(), payment.getAmount(),
                payment.getVerifiedAt(), ticket.getTicketNumber());
    }
    private List<BookingSeat> ensureSeatsPayable(PassengerTripBooking booking) {
        List<BookingSeat> seats = seatRepository.findWithLockByBookingOrderBySeatNumberAsc(booking);
        LocalDateTime now = LocalDateTime.now();
        if (seats.isEmpty() || seats.stream().anyMatch(seat -> seat.getStatus() != BookingSeatStatus.HELD
                || seat.getActiveSeatNumber() == null))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Selected seats are no longer held.");
        if (seats.stream().anyMatch(seat -> !seat.getHoldExpiresAt().isAfter(now))) {
            seats.forEach(seat -> seat.release(BookingSeatStatus.RELEASED));
            booking.setStatus(BookingStatus.EXPIRED);
            booking.setCancelledAt(now);
            seatRepository.saveAll(seats);
            bookingRepository.saveAndFlush(booking);
            throw new BookingExpiredException();
        }
        return seats;
    }
    private void rejectClosedBooking(PassengerTripBooking booking) {
        if (booking.getStatus() == BookingStatus.EXPIRED) throw new ResponseStatusException(HttpStatus.CONFLICT, "This booking has expired.");
        if (booking.getStatus() == BookingStatus.CANCELLED) throw new ResponseStatusException(HttpStatus.CONFLICT, "This booking has been cancelled.");
        if (booking.getStatus() == BookingStatus.CONFIRMED) throw new ResponseStatusException(HttpStatus.CONFLICT, "This booking has already been paid.");
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) throw new ResponseStatusException(HttpStatus.CONFLICT, "This booking cannot be paid.");
    }
    private PassengerTripBooking ownedLockedBooking(String reference, User passenger) {
        if (!notBlank(reference)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found.");
        return bookingRepository.findOwnedByReferenceForPayment(reference.trim(), passenger.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found."));
    }
    private User requirePassenger(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user not found"));
        if (!"PASSENGER".equalsIgnoreCase(user.getRole())) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Passenger access is required");
        return user;
    }
    private void requireConfigured() {
        if (!properties.isEnabled()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Khalti payments are not configured.");
    }
    private boolean notBlank(String value) { return value != null && !value.isBlank(); }
    private static final class BookingExpiredException extends ResponseStatusException {
        private BookingExpiredException() { super(HttpStatus.CONFLICT, "This booking has expired."); }
    }
    private static final class VerificationRejectedException extends ResponseStatusException {
        private VerificationRejectedException(String message) { super(HttpStatus.CONFLICT, message); }
    }
}
