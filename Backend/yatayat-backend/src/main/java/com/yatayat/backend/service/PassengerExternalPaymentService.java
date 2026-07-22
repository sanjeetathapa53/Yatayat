package com.yatayat.backend.service;

import com.yatayat.backend.dto.*;
import com.yatayat.backend.entity.*;
import com.yatayat.backend.payment.ExternalPaymentVerifier;
import com.yatayat.backend.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class PassengerExternalPaymentService {
    private static final DateTimeFormatter REFERENCE_DATE = DateTimeFormatter.BASIC_ISO_DATE;
    private final UserRepository userRepository;
    private final PassengerTripBookingRepository bookingRepository;
    private final BookingSeatRepository seatRepository;
    private final PaymentRepository paymentRepository;
    private final PassengerTicketService ticketService;
    private final ExternalPaymentVerifier verifier;

    public PassengerExternalPaymentService(UserRepository userRepository,
                                           PassengerTripBookingRepository bookingRepository,
                                           BookingSeatRepository seatRepository,
                                           PaymentRepository paymentRepository,
                                           PassengerTicketService ticketService,
                                           ExternalPaymentVerifier verifier) {
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
        this.seatRepository = seatRepository;
        this.paymentRepository = paymentRepository;
        this.ticketService = ticketService;
        this.verifier = verifier;
    }

    @Transactional(dontRollbackOn = BookingExpiredException.class)
    public ExternalPaymentInitiationResponse initiate(String email, String reference, PaymentMethod provider) {
        User passenger = requirePassenger(email);
        requireExternal(provider);
        PassengerTripBooking booking = ownedLockedBooking(reference, passenger);
        rejectClosedBooking(booking);
        paymentRepository.findFirstByBookingAndStatusOrderByCreatedAtDesc(booking, PaymentStatus.SUCCESS)
                .ifPresent(payment -> { throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "This booking has already been paid."); });

        Payment existing = paymentRepository
                .findFirstByBookingAndPaymentMethodAndStatusOrderByCreatedAtDesc(
                        booking, provider, PaymentStatus.INITIATED)
                .orElse(null);
        if (existing != null) return initiationResponse(existing, booking);

        ensureSeatsPayable(booking, false);
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setPassenger(passenger);
        payment.setAmount(booking.getTotalFare());
        payment.setPaymentMethod(provider);
        payment.setStatus(PaymentStatus.INITIATED);
        payment.setTransactionReference(generateReference(provider));
        payment.setInitiatedAt(LocalDateTime.now());
        try {
            return initiationResponse(paymentRepository.saveAndFlush(payment), booking);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A payment attempt is already active for this booking.");
        }
    }

    @Transactional(dontRollbackOn = {PaymentNotVerifiedException.class, BookingExpiredException.class})
    public ExternalPaymentVerificationResponse verify(String email, String reference,
                                                      PaymentMethod provider,
                                                      ExternalPaymentVerificationRequest request) {
        User passenger = requirePassenger(email);
        requireExternal(provider);
        if (request == null || blank(request.paymentReference()) || blank(request.providerTransactionId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Payment reference and provider transaction ID are required.");
        }
        PassengerTripBooking booking = ownedLockedBooking(reference, passenger);
        Payment payment = paymentRepository.findByBookingAndPaymentMethodAndTransactionReference(
                        booking, provider, request.paymentReference().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment attempt not found."));

        if (payment.getStatus() == PaymentStatus.SUCCESS) return successfulResponse(payment, booking);
        rejectClosedBooking(booking);
        List<BookingSeat> seats = ensureSeatsPayable(booking, true);
        ExternalPaymentVerifier.VerificationResult result = verifier.verify(
                provider, payment.getTransactionReference(), request.providerTransactionId().trim(),
                booking.getBookingReference(), booking.getTotalFare());
        if (!result.verified() || result.verifiedAmount() == null
                || result.verifiedAmount().compareTo(booking.getTotalFare()) != 0) {
            payment.setFailureReason(result.failureReason() == null
                    ? "Provider did not verify this payment." : result.failureReason());
            paymentRepository.saveAndFlush(payment);
            throw new PaymentNotVerifiedException(payment.getFailureReason());
        }

        payment.setProviderTransactionId(request.providerTransactionId());
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaidAt(LocalDateTime.now());
        payment.setVerifiedAt(payment.getPaidAt());
        payment.setFailureReason(null);
        booking.setStatus(BookingStatus.CONFIRMED);
        seats.forEach(seat -> {
            seat.setStatus(BookingSeatStatus.CONFIRMED);
            seat.setHoldExpiresAt(payment.getPaidAt());
        });
        try {
            paymentRepository.save(payment);
            seatRepository.saveAll(seats);
            bookingRepository.saveAndFlush(booking);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Provider transaction has already been processed.");
        }
        Ticket ticket = ticketService.issueForConfirmedBooking(booking);
        ticketService.scheduleAutomaticEmailAfterCommit(ticket.getTicketNumber());
        return successfulResponse(payment, booking);
    }

    private List<BookingSeat> ensureSeatsPayable(PassengerTripBooking booking, boolean lock) {
        List<BookingSeat> seats = lock
                ? seatRepository.findWithLockByBookingOrderBySeatNumberAsc(booking)
                : seatRepository.findByBookingOrderBySeatNumberAsc(booking);
        LocalDateTime now = LocalDateTime.now();
        if (seats.isEmpty() || seats.stream().anyMatch(seat -> seat.getStatus() != BookingSeatStatus.HELD
                || seat.getActiveSeatNumber() == null)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Selected seats are no longer held.");
        }
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
        if (booking.getStatus() == BookingStatus.EXPIRED)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This booking has expired.");
        if (booking.getStatus() == BookingStatus.CANCELLED)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This booking has been cancelled.");
        if (booking.getStatus() == BookingStatus.CONFIRMED)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This booking has already been paid.");
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This booking cannot be paid.");
    }

    private PassengerTripBooking ownedLockedBooking(String reference, User passenger) {
        if (blank(reference)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found.");
        return bookingRepository.findOwnedByReferenceForPayment(reference.trim(), passenger.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found."));
    }

    private User requirePassenger(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Authenticated user not found"));
        if (!"PASSENGER".equalsIgnoreCase(user.getRole()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Passenger access is required");
        return user;
    }

    private ExternalPaymentInitiationResponse initiationResponse(Payment payment,
                                                                 PassengerTripBooking booking) {
        boolean configured = verifier.isConfigured(payment.getPaymentMethod());
        String message = configured
                ? "Payment initiation is ready for the configured provider adapter."
                : payment.getPaymentMethod().name()
                + " sandbox credentials are not configured. No payment has been taken.";
        return new ExternalPaymentInitiationResponse(
                booking.getBookingReference(), payment.getPaymentMethod().name(),
                payment.getTransactionReference(), payment.getAmount(), payment.getStatus().name(),
                payment.getInitiatedAt(), configured, null, message);
    }

    private ExternalPaymentVerificationResponse successfulResponse(Payment payment,
                                                                    PassengerTripBooking booking) {
        Ticket ticket = ticketService.issueForConfirmedBooking(booking);
        return new ExternalPaymentVerificationResponse(
                booking.getBookingReference(), booking.getStatus().name(),
                payment.getPaymentMethod().name(), payment.getTransactionReference(),
                payment.getProviderTransactionId(), payment.getStatus().name(), payment.getAmount(),
                payment.getVerifiedAt(), ticket.getTicketNumber());
    }

    private String generateReference(PaymentMethod provider) {
        for (int attempt = 0; attempt < 8; attempt++) {
            String value = provider.name() + "-" + LocalDate.now().format(REFERENCE_DATE) + "-"
                    + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
            if (!paymentRepository.existsByTransactionReference(value)) return value;
        }
        throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Payment reference could not be generated. Please try again.");
    }

    private void requireExternal(PaymentMethod method) {
        if (method != PaymentMethod.ESEWA && method != PaymentMethod.KHALTI)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported external payment provider.");
    }
    private boolean blank(String value) { return value == null || value.isBlank(); }

    private static final class PaymentNotVerifiedException extends ResponseStatusException {
        private PaymentNotVerifiedException(String reason) { super(HttpStatus.CONFLICT, reason); }
    }

    private static final class BookingExpiredException extends ResponseStatusException {
        private BookingExpiredException() { super(HttpStatus.CONFLICT, "This booking has expired."); }
    }
}
