package com.yatayat.backend.service;

import com.yatayat.backend.dto.TicketResponse;
import com.yatayat.backend.entity.*;
import com.yatayat.backend.repository.PaymentRepository;
import com.yatayat.backend.repository.TicketRepository;
import com.yatayat.backend.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.transaction.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PassengerTicketService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PassengerTicketService.class);
    private static final DateTimeFormatter TICKET_DATE = DateTimeFormatter.BASIC_ISO_DATE;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final TicketPdfService ticketPdfService;
    private final EmailService emailService;
    private final NotificationService notificationService;

    public PassengerTicketService(TicketRepository ticketRepository,
                                  UserRepository userRepository,
                                  PaymentRepository paymentRepository,
                                  TicketPdfService ticketPdfService,
                                  EmailService emailService,
                                  NotificationService notificationService) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.paymentRepository = paymentRepository;
        this.ticketPdfService = ticketPdfService;
        this.emailService = emailService;
        this.notificationService = notificationService;
    }

    @Transactional
    public Ticket issueForConfirmedBooking(PassengerTripBooking booking) {
        if (booking == null || booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Complete payment before viewing the ticket.");
        }
        Ticket ticket = ticketRepository.findByBooking(booking)
                .orElseGet(() -> createTicket(booking));
        notificationService.bookingConfirmed(booking);
        paymentRepository.findFirstByBookingAndStatusOrderByCreatedAtDesc(
                booking, PaymentStatus.SUCCESS)
                .ifPresent(payment -> notificationService.paymentSuccessful(booking, payment));
        notificationService.ticketGenerated(booking, ticket);
        return ticket;
    }

    public TicketResponse getByBookingReference(String email, String bookingReference) {
        requirePassenger(email);
        Ticket ticket = ticketRepository
                .findByBookingBookingReferenceAndBookingPassengerEmailIgnoreCase(bookingReference, email)
                .orElseThrow(this::ticketNotFound);
        ensureViewable(ticket);
        return toResponse(ticket);
    }

    public TicketResponse getByTicketNumber(String email, String ticketNumber) {
        requirePassenger(email);
        Ticket ticket = ownedTicket(email, ticketNumber);
        ensureViewable(ticket);
        return toResponse(ticket);
    }

    public byte[] pdf(String email, String ticketNumber) {
        requirePassenger(email);
        Ticket ticket = ownedTicket(email, ticketNumber);
        ensureViewable(ticket);
        return ticketPdfService.generatePassengerTripTicketPdf(toResponse(ticket));
    }

    public void sendEmail(String email, String ticketNumber) {
        User passenger = requirePassenger(email);
        if (passenger.getEmail() == null || passenger.getEmail().isBlank()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No registered passenger email is available.");
        }
        Ticket ticket = ownedTicket(email, ticketNumber);
        ensureViewable(ticket);
        TicketResponse response = toResponse(ticket);
        byte[] pdf = ticketPdfService.generatePassengerTripTicketPdf(response);
        try {
            emailService.sendPassengerTripTicketEmail(passenger.getEmail(), response, pdf);
        } catch (MessagingException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to send the e-ticket. Please try again.");
        }
    }

    public void scheduleAutomaticEmailAfterCommit(String ticketNumber) {
        if (ticketNumber == null || ticketNumber.isBlank()) return;
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            sendAutomaticEmail(ticketNumber);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                sendAutomaticEmail(ticketNumber);
            }
        });
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void sendAutomaticEmail(String ticketNumber) {
        Ticket ticket = ticketRepository.findByTicketNumber(ticketNumber).orElse(null);
        if (ticket == null || ticket.getAutoEmailStatus() == TicketEmailStatus.SENT) return;
        ticket.setLastEmailAttemptAt(LocalDateTime.now());
        try {
            ensureViewable(ticket);
            TicketResponse response = toResponse(ticket);
            byte[] pdf = ticketPdfService.generatePassengerTripTicketPdf(response);
            emailService.sendPassengerTripTicketEmail(ticket.getBooking().getPassenger().getEmail(), response, pdf);
            ticket.setAutoEmailStatus(TicketEmailStatus.SENT);
            ticket.setAutoEmailSentAt(LocalDateTime.now());
        } catch (Exception exception) {
            ticket.setAutoEmailStatus(TicketEmailStatus.FAILED);
            LOGGER.warn("Automatic e-ticket email failed for ticket {}", ticket.getTicketNumber());
        }
        ticketRepository.save(ticket);
    }

    private Ticket createTicket(PassengerTripBooking booking) {
        Ticket ticket = new Ticket();
        LocalDateTime now = LocalDateTime.now();
        ticket.setTicketNumber(generateTicketNumber());
        ticket.setBooking(booking);
        ticket.setStatus(TicketStatus.VALID);
        ticket.setAutoEmailStatus(TicketEmailStatus.PENDING);
        ticket.setQrTokenHash(generateQrTokenHash());
        ticket.setIssuedAt(now);
        ticket.setValidFrom(now);
        ticket.setValidUntil(validUntil(booking.getScheduledTrip()));
        try {
            return ticketRepository.saveAndFlush(ticket);
        } catch (DataIntegrityViolationException exception) {
            return ticketRepository.findByBooking(booking)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                            "Ticket could not be generated. Please try again."));
        }
    }

    private Ticket ownedTicket(String email, String ticketNumber) {
        if (ticketNumber == null || ticketNumber.isBlank()) throw ticketNotFound();
        return ticketRepository.findByTicketNumberAndBookingPassengerEmailIgnoreCase(ticketNumber.trim(), email)
                .orElseThrow(this::ticketNotFound);
    }

    private void ensureViewable(Ticket ticket) {
        PassengerTripBooking booking = ticket.getBooking();
        if (booking.getStatus() == BookingStatus.PENDING_PAYMENT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Complete payment before viewing the ticket.");
        }
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            ticket.setStatus(TicketStatus.CANCELLED);
            if (ticket.getCancelledAt() == null) ticket.setCancelledAt(booking.getCancelledAt());
        } else if (ticket.getStatus() == TicketStatus.VALID
                && ticket.getValidUntil().isBefore(LocalDateTime.now())) {
            ticket.setStatus(TicketStatus.EXPIRED);
        }
    }

    private TicketResponse toResponse(Ticket ticket) {
        PassengerTripBooking booking = ticket.getBooking();
        ScheduledTrip trip = booking.getScheduledTrip();
        Payment payment = successfulPayment(booking).orElse(null);
        String boardingPoint = trip.getBoardingNotes() == null || trip.getBoardingNotes().isBlank()
                ? trip.getRoute().getOrigin() : trip.getBoardingNotes();
        return new TicketResponse(
                ticket.getTicketNumber(), booking.getBookingReference(), ticket.getStatus().name(),
                booking.getPassengerName(), trip.getRoute().getOrigin(), trip.getRoute().getDestination(),
                trip.getOperator().getName(), trip.getBus().getBusName(), trip.getBus().getBusNumber(),
                trip.getDepartureAt(), trip.getDepartureAt(), boardingPoint, trip.getRoute().getDestination(),
                seatNumbers(booking), booking.getTotalFare(),
                payment == null ? null : payment.getPaymentMethod().name(),
                payment == null ? null : payment.getTransactionReference(),
                ticket.getIssuedAt(), ticket.getValidFrom(), ticket.getValidUntil(),
                qrPayload(ticket)
        );
    }

    private Optional<Payment> successfulPayment(PassengerTripBooking booking) {
        return paymentRepository.findFirstByBookingAndStatusOrderByCreatedAtDesc(
                booking, PaymentStatus.SUCCESS);
    }

    private User requirePassenger(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Authenticated user not found"));
        if (!"PASSENGER".equalsIgnoreCase(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Passenger access is required");
        }
        return user;
    }

    private String generateTicketNumber() {
        for (int attempt = 0; attempt < 8; attempt++) {
            String suffix = UUID.randomUUID().toString().replace("-", "")
                    .substring(0, 6).toUpperCase();
            String number = "YT-TKT-" + LocalDate.now().format(TICKET_DATE) + "-" + suffix;
            if (!ticketRepository.existsByTicketNumber(number)) return number;
        }
        throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Ticket number could not be generated. Please try again.");
    }

    private String generateQrTokenHash() {
        for (int attempt = 0; attempt < 8; attempt++) {
            byte[] bytes = new byte[32];
            SECURE_RANDOM.nextBytes(bytes);
            String hash = sha256(HexFormat.of().formatHex(bytes));
            if (!ticketRepository.existsByQrTokenHash(hash)) return hash;
        }
        throw new ResponseStatusException(HttpStatus.CONFLICT,
                "QR token could not be generated. Please try again.");
    }

    private String qrPayload(Ticket ticket) {
        return """
                {"version":1,"ticketNumber":"%s","token":"%s"}
                """.formatted(ticket.getTicketNumber(), ticket.getQrTokenHash()).trim();
    }

    private LocalDateTime validUntil(ScheduledTrip trip) {
        if (trip.getEstimatedArrivalAt() != null) return trip.getEstimatedArrivalAt().plusHours(2);
        return trip.getDepartureAt().plusHours(12);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to generate secure QR token");
        }
    }

    private List<String> seatNumbers(PassengerTripBooking booking) {
        if (booking.getSeats() == null || booking.getSeats().isEmpty()) return List.of();
        return booking.getSeats().stream().map(BookingSeat::getSeatNumber).sorted().toList();
    }

    private ResponseStatusException ticketNotFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found.");
    }
}
