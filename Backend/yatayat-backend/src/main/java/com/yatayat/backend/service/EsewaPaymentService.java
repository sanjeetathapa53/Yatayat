package com.yatayat.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class EsewaPaymentService {
    private static final String SIGNED_REQUEST_FIELDS =
            "total_amount,transaction_uuid,product_code";
    private static final Set<String> ALLOWED_CALLBACK_FIELDS = Set.of(
            "transaction_code", "status", "total_amount", "transaction_uuid",
            "product_code", "signed_field_names");
    private static final Set<String> REQUIRED_CALLBACK_FIELDS = Set.of(
            "transaction_code", "status", "total_amount", "transaction_uuid",
            "product_code", "signed_field_names");
    private static final DateTimeFormatter REFERENCE_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final EsewaProperties properties;
    private final EsewaGateway gateway;
    private final EsewaSignatureService signatures;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final PassengerTripBookingRepository bookingRepository;
    private final BookingSeatRepository seatRepository;
    private final PaymentRepository paymentRepository;
    private final PassengerTicketService ticketService;

    public EsewaPaymentService(EsewaProperties properties, EsewaGateway gateway,
                               EsewaSignatureService signatures, ObjectMapper objectMapper,
                               UserRepository userRepository,
                               PassengerTripBookingRepository bookingRepository,
                               BookingSeatRepository seatRepository,
                               PaymentRepository paymentRepository,
                               PassengerTicketService ticketService) {
        this.properties = properties;
        this.gateway = gateway;
        this.signatures = signatures;
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
        this.seatRepository = seatRepository;
        this.paymentRepository = paymentRepository;
        this.ticketService = ticketService;
    }

    @Transactional(dontRollbackOn = BookingExpiredException.class)
    public EsewaPaymentInitiationResponse initiate(String email, String reference) {
        requireConfigured();
        User passenger = requirePassenger(email);
        PassengerTripBooking booking = ownedLockedBooking(reference, passenger);
        Payment successful = paymentRepository
                .findFirstByBookingAndStatusOrderByCreatedAtDesc(booking, PaymentStatus.SUCCESS)
                .orElse(null);
        if (booking.getStatus() == BookingStatus.CONFIRMED && successful != null) {
            return initiationResponse(successful, booking, Map.of(),
                    "This booking has already been paid.");
        }
        rejectClosedBooking(booking);
        List<BookingSeat> seats = ensureSeatsPayable(booking);

        Payment existing = paymentRepository
                .findFirstByBookingAndPaymentMethodAndStatusOrderByCreatedAtDesc(
                        booking, PaymentMethod.ESEWA, PaymentStatus.INITIATED)
                .orElse(null);
        if (existing != null && notBlank(existing.getProviderPaymentId())) {
            return initiationResponse(existing, booking, formFields(existing, booking),
                    "Continue to eSewa sandbox checkout.");
        }

        LocalDateTime now = LocalDateTime.now();
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setPassenger(passenger);
        payment.setAmount(booking.getTotalFare());
        payment.setPaymentMethod(PaymentMethod.ESEWA);
        payment.setStatus(PaymentStatus.INITIATED);
        payment.setTransactionReference(generateInternalReference());
        payment.setProviderPaymentId(generateTransactionUuid());
        payment.setProviderPaymentUrl(properties.getFormUrl());
        payment.setProviderExpiresAt(seats.stream().map(BookingSeat::getHoldExpiresAt)
                .min(LocalDateTime::compareTo).orElse(now));
        payment.setInitiatedAt(now);
        try {
            paymentRepository.saveAndFlush(payment);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "eSewa payment initiation conflicts with an existing attempt.");
        }
        return initiationResponse(payment, booking, formFields(payment, booking),
                "Continue to eSewa sandbox checkout.");
    }

    @Transactional(dontRollbackOn = {
            BookingExpiredException.class, VerificationRejectedException.class
    })
    public ExternalPaymentVerificationResponse verify(
            String email, String reference, EsewaPaymentVerificationRequest request) {
        requireConfigured();
        User passenger = requirePassenger(email);
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "eSewa verification information is required.");
        }

        PassengerTripBooking booking = ownedLockedBooking(reference, passenger);
        VerifiedCallbackData callback = notBlank(request.data())
                ? verifyCallbackData(request.data().trim()) : null;
        String transactionUuid = callback == null
                ? cleanRequiredTransactionUuid(request.transactionUuid())
                : callback.transactionUuid();
        Payment payment = paymentRepository.findByBookingAndPaymentMethodAndProviderPaymentId(
                        booking, PaymentMethod.ESEWA, transactionUuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "eSewa payment attempt not found."));
        if (!transactionUuid.equals(payment.getProviderPaymentId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "eSewa transaction UUID does not match this booking.");
        }
        if (payment.getPaymentMethod() != PaymentMethod.ESEWA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Payment provider does not match eSewa.");
        }
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return successfulResponse(payment, booking);
        }

        rejectClosedBooking(booking);
        List<BookingSeat> seats = ensureSeatsPayable(booking);
        if (callback != null) {
            validateVerifiedCallback(payment, booking, callback);
        }

        String expectedAmount = formatAmount(booking.getTotalFare());
        EsewaGateway.StatusResult lookup = gateway.lookup(
                properties.getProductCode().trim(), expectedAmount, transactionUuid);
        validateLookupIdentity(payment, booking, lookup);
        String status = lookup.status().trim().toUpperCase(Locale.ROOT);
        if (!"COMPLETE".equals(status)) {
            return switch (status) {
                case "PENDING", "AMBIGUOUS", "AMBIGIOUS" ->
                        nonSuccessful(payment, booking, PaymentStatus.PENDING,
                                "eSewa payment is pending confirmation.");
                case "CANCELED", "CANCELLED" ->
                        nonSuccessful(payment, booking, PaymentStatus.CANCELLED,
                                "eSewa payment was cancelled.");
                case "FULL_REFUND", "PARTIAL_REFUND", "REFUNDED" ->
                        nonSuccessful(payment, booking, PaymentStatus.REFUNDED,
                                "eSewa reports this payment as refunded.");
                case "NOT_FOUND", "EXPIRED" ->
                        nonSuccessful(payment, booking, PaymentStatus.EXPIRED,
                                "eSewa payment was not found or expired.");
                default -> nonSuccessful(payment, booking, PaymentStatus.FAILED,
                        "eSewa payment was not completed.");
            };
        }
        if (callback == null) {
            rejectVerification(payment,
                    "Signed eSewa callback data is required to confirm a successful payment.");
        }
        if (!notBlank(lookup.referenceId())) {
            rejectVerification(payment, "eSewa did not return a provider reference ID.");
        }

        LocalDateTime verifiedAt = LocalDateTime.now();
        payment.setProviderTransactionId(lookup.referenceId());
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
                    "eSewa transaction has already been processed.");
        }
        Ticket ticket = ticketService.issueForConfirmedBooking(booking);
        ticketService.scheduleAutomaticEmailAfterCommit(ticket.getTicketNumber());
        return successfulResponse(payment, booking);
    }

    private Map<String, String> formFields(Payment payment, PassengerTripBooking booking) {
        String amount = formatAmount(booking.getTotalFare());
        String transactionUuid = payment.getProviderPaymentId();
        String productCode = properties.getProductCode().trim();
        String message = "total_amount=" + amount
                + ",transaction_uuid=" + transactionUuid
                + ",product_code=" + productCode;
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("amount", amount);
        fields.put("tax_amount", "0");
        fields.put("total_amount", amount);
        fields.put("transaction_uuid", transactionUuid);
        fields.put("product_code", productCode);
        fields.put("product_service_charge", "0");
        fields.put("product_delivery_charge", "0");
        fields.put("success_url", callbackUrl("success", booking, null));
        fields.put("failure_url", callbackUrl("failure", booking, transactionUuid));
        fields.put("signed_field_names", SIGNED_REQUEST_FIELDS);
        fields.put("signature", signatures.sign(message, properties.getSecretKey()));
        return Collections.unmodifiableMap(fields);
    }

    private String callbackUrl(String outcome, PassengerTripBooking booking,
                               String fallbackTransactionUuid) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(properties.getFrontendBaseUrl())
                .path("/passenger/payments/esewa/callback")
                .pathSegment(outcome, booking.getBookingReference());
        if (notBlank(fallbackTransactionUuid)) {
            builder.pathSegment(fallbackTransactionUuid);
        }
        return builder.build().encode().toUriString();
    }

    private VerifiedCallbackData verifyCallbackData(String encodedData) {
        if (encodedData.length() > 16_384) {
            throw verificationRejected("eSewa callback data is too large.");
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(encodedData.replace(' ', '+'));
            JsonNode root = objectMapper.readTree(new String(decoded, StandardCharsets.UTF_8));
            String signedNamesValue = requiredText(root, "signed_field_names");
            String[] signedNames = Arrays.stream(signedNamesValue.split(","))
                    .map(String::trim).toArray(String[]::new);
            Set<String> signedSet = new HashSet<>(Arrays.asList(signedNames));
            if (!signedSet.containsAll(REQUIRED_CALLBACK_FIELDS)
                    || signedSet.size() != signedNames.length
                    || Arrays.stream(signedNames).anyMatch(name ->
                    !ALLOWED_CALLBACK_FIELDS.contains(name))) {
                throw verificationRejected("eSewa callback signed fields are invalid.");
            }
            String canonical = Arrays.stream(signedNames)
                    .map(name -> name + "=" + requiredText(root, name))
                    .reduce((left, right) -> left + "," + right)
                    .orElseThrow();
            if (!signatures.verify(canonical, requiredText(root, "signature"),
                    properties.getSecretKey())) {
                throw verificationRejected("eSewa callback signature is invalid.");
            }
            String transactionUuid = requiredText(root, "transaction_uuid");
            String productCode = requiredText(root, "product_code");
            if (!properties.getProductCode().trim().equals(productCode)) {
                throw verificationRejected("eSewa callback product code does not match.");
            }
            BigDecimal callbackAmount = new BigDecimal(requiredText(root, "total_amount")
                    .replace(",", ""));
            return new VerifiedCallbackData(transactionUuid, productCode, callbackAmount);
        } catch (VerificationRejectedException exception) {
            throw exception;
        } catch (RuntimeException | java.io.IOException exception) {
            throw verificationRejected("eSewa callback data is invalid.");
        }
    }

    private void validateVerifiedCallback(Payment payment,
                                          PassengerTripBooking booking,
                                          VerifiedCallbackData callback) {
        if (!payment.getProviderPaymentId().equals(callback.transactionUuid())) {
            rejectVerification(payment, "eSewa callback transaction UUID does not match.");
        }
        if (!properties.getProductCode().trim().equals(callback.productCode())) {
            rejectVerification(payment, "eSewa callback product code does not match.");
        }
        if (callback.totalAmount().compareTo(booking.getTotalFare()) != 0) {
            rejectVerification(payment, "eSewa callback amount does not match.");
        }
    }

    private String requiredText(JsonNode root, String field) {
        JsonNode value = root.get(field);
        if (value == null || value.isNull() || value.asText().isBlank()) {
            throw new IllegalArgumentException("Missing callback field.");
        }
        return value.asText().trim();
    }

    private void validateLookupIdentity(Payment payment, PassengerTripBooking booking,
                                        EsewaGateway.StatusResult lookup) {
        if (lookup == null || !notBlank(lookup.status())) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "eSewa returned an invalid status response.");
        }
        if (notBlank(lookup.transactionUuid())
                && !payment.getProviderPaymentId().equals(lookup.transactionUuid().trim())) {
            rejectVerification(payment, "eSewa status transaction UUID does not match.");
        }
        if (notBlank(lookup.productCode())
                && !properties.getProductCode().trim().equals(lookup.productCode().trim())) {
            rejectVerification(payment, "eSewa status product code does not match.");
        }
        if (lookup.totalAmount() == null
                || lookup.totalAmount().compareTo(booking.getTotalFare()) != 0) {
            rejectVerification(payment, "eSewa status amount does not match.");
        }
    }

    private ExternalPaymentVerificationResponse nonSuccessful(
            Payment payment, PassengerTripBooking booking,
            PaymentStatus status, String message) {
        payment.setStatus(status);
        payment.setFailureReason(message);
        paymentRepository.saveAndFlush(payment);
        return new ExternalPaymentVerificationResponse(
                booking.getBookingReference(), booking.getStatus().name(), "ESEWA",
                payment.getTransactionReference(), payment.getProviderPaymentId(),
                status.name(), null, null, null);
    }

    private void rejectVerification(Payment payment, String message) {
        payment.setFailureReason(message);
        paymentRepository.saveAndFlush(payment);
        throw new VerificationRejectedException(message);
    }

    private VerificationRejectedException verificationRejected(String message) {
        return new VerificationRejectedException(message);
    }

    private String cleanRequiredTransactionUuid(String value) {
        if (!notBlank(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "eSewa transaction UUID is required when callback data is absent.");
        }
        return value.trim();
    }

    private EsewaPaymentInitiationResponse initiationResponse(
            Payment payment, PassengerTripBooking booking,
            Map<String, String> fields, String message) {
        return new EsewaPaymentInitiationResponse(
                booking.getBookingReference(), "ESEWA",
                payment.getTransactionReference(), payment.getAmount(),
                payment.getStatus().name(), payment.getInitiatedAt(), true,
                properties.getFormUrl(), fields, message);
    }

    private ExternalPaymentVerificationResponse successfulResponse(
            Payment payment, PassengerTripBooking booking) {
        Ticket ticket = ticketService.issueForConfirmedBooking(booking);
        return new ExternalPaymentVerificationResponse(
                booking.getBookingReference(), booking.getStatus().name(), "ESEWA",
                payment.getTransactionReference(), payment.getProviderTransactionId(),
                payment.getStatus().name(), payment.getAmount(),
                payment.getVerifiedAt(), ticket.getTicketNumber());
    }

    private List<BookingSeat> ensureSeatsPayable(PassengerTripBooking booking) {
        List<BookingSeat> seats =
                seatRepository.findWithLockByBookingOrderBySeatNumberAsc(booking);
        LocalDateTime now = LocalDateTime.now();
        if (seats.isEmpty() || seats.stream().anyMatch(seat ->
                seat.getStatus() != BookingSeatStatus.HELD
                        || seat.getActiveSeatNumber() == null)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Selected seats are no longer held.");
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
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This booking has expired.");
        if (booking.getStatus() == BookingStatus.CANCELLED)
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This booking has been cancelled.");
        if (booking.getStatus() == BookingStatus.CONFIRMED)
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This booking has already been paid.");
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT)
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This booking cannot be paid.");
    }

    private PassengerTripBooking ownedLockedBooking(String reference, User passenger) {
        if (!notBlank(reference))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found.");
        return bookingRepository.findOwnedByReferenceForPayment(
                        reference.trim(), passenger.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Booking not found."));
    }

    private User requirePassenger(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Authenticated user not found"));
        if (!"PASSENGER".equalsIgnoreCase(user.getRole())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Passenger access is required");
        }
        return user;
    }

    private void requireConfigured() {
        if (!properties.isEnabled()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "eSewa payments are not configured.");
        }
    }

    private String generateInternalReference() {
        for (int attempt = 0; attempt < 8; attempt++) {
            String value = "ESEWA-" + LocalDate.now().format(REFERENCE_DATE) + "-"
                    + UUID.randomUUID().toString().replace("-", "")
                    .substring(0, 8).toUpperCase(Locale.ROOT);
            if (!paymentRepository.existsByTransactionReference(value)) return value;
        }
        throw new ResponseStatusException(HttpStatus.CONFLICT,
                "eSewa payment reference could not be generated.");
    }

    private String generateTransactionUuid() {
        return LocalDate.now().format(REFERENCE_DATE) + "-"
                + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 20).toUpperCase(Locale.ROOT);
    }

    public static String formatAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Booking amount is invalid.");
        }
        return amount.setScale(2, RoundingMode.UNNECESSARY).toPlainString();
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private record VerifiedCallbackData(
            String transactionUuid,
            String productCode,
            BigDecimal totalAmount
    ) {}

    private static final class BookingExpiredException extends ResponseStatusException {
        private BookingExpiredException() {
            super(HttpStatus.CONFLICT, "This booking has expired.");
        }
    }

    private static final class VerificationRejectedException
            extends ResponseStatusException {
        private VerificationRejectedException(String message) {
            super(HttpStatus.CONFLICT, message);
        }
    }
}
