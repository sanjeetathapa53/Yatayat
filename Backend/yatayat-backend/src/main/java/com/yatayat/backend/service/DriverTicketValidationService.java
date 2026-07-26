package com.yatayat.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yatayat.backend.dto.DriverTicketValidationResponse;
import com.yatayat.backend.dto.DriverTripManifestResponse;
import com.yatayat.backend.entity.*;
import com.yatayat.backend.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DriverTicketValidationService {
    public static final int MAX_QR_PAYLOAD_LENGTH = 2048;
    private static final List<TripStatus> BOARDABLE_TRIP_STATUSES =
            List.of(TripStatus.SCHEDULED, TripStatus.BOARDING, TripStatus.IN_PROGRESS);

    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final DriverOperatorAssociationRepository associationRepository;
    private final TicketRepository ticketRepository;
    private final ScheduledTripRepository scheduledTripRepository;
    private final PaymentRepository paymentRepository;
    private final TicketQrTokenService qrTokenService;

    public DriverTicketValidationService(ObjectMapper objectMapper,
                                         UserRepository userRepository,
                                         DriverProfileRepository driverProfileRepository,
                                         DriverOperatorAssociationRepository associationRepository,
                                         TicketRepository ticketRepository,
                                         ScheduledTripRepository scheduledTripRepository,
                                         PaymentRepository paymentRepository,
                                         TicketQrTokenService qrTokenService) {
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
        this.driverProfileRepository = driverProfileRepository;
        this.associationRepository = associationRepository;
        this.ticketRepository = ticketRepository;
        this.scheduledTripRepository = scheduledTripRepository;
        this.paymentRepository = paymentRepository;
        this.qrTokenService = qrTokenService;
    }

    @Transactional(dontRollbackOn = TicketValidationException.class)
    public DriverTicketValidationResponse validate(String driverEmail, String qrPayload) {
        DriverProfile driver = requireApprovedDriver(driverEmail);
        requireAnyActiveAssociation(driver);
        ParsedQr parsedQr = parseQr(qrPayload);
        Ticket ticket = ticketRepository.findByTicketNumberForValidation(parsedQr.ticketNumber())
                .orElseThrow(() -> validation(HttpStatus.BAD_REQUEST, "INVALID_QR",
                        "This QR ticket could not be verified."));

        if (!qrTokenService.matches(
                ticket.getTicketNumber(), parsedQr.token(), ticket.getQrTokenHash())) {
            throw validation(HttpStatus.BAD_REQUEST, "INVALID_QR",
                    "This QR ticket could not be verified.");
        }

        PassengerTripBooking booking = ticket.getBooking();
        ScheduledTrip trip = booking.getScheduledTrip();
        requireAssignedDriver(driver, trip);

        if (ticket.getStatus() == TicketStatus.USED) {
            throw validation(HttpStatus.CONFLICT, "ALREADY_USED",
                    "This ticket was already used at " + formatUsedAt(ticket.getUsedAt()) + ".");
        }
        if (ticket.getStatus() == TicketStatus.CANCELLED || booking.getStatus() == BookingStatus.CANCELLED) {
            if (ticket.getStatus() != TicketStatus.CANCELLED) {
                ticket.setStatus(TicketStatus.CANCELLED);
                ticket.setCancelledAt(booking.getCancelledAt() == null
                        ? LocalDateTime.now() : booking.getCancelledAt());
                ticketRepository.saveAndFlush(ticket);
            }
            throw validation(HttpStatus.CONFLICT, "CANCELLED",
                    "This ticket has been cancelled.");
        }
        if (ticket.getStatus() == TicketStatus.EXPIRED) {
            throw validation(HttpStatus.CONFLICT, "EXPIRED",
                    "This ticket has expired.");
        }
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw validation(HttpStatus.CONFLICT, "BOOKING_NOT_CONFIRMED",
                    "This booking is not confirmed.");
        }
        if (!paymentRepository.existsByBookingAndStatus(booking, PaymentStatus.SUCCESS)) {
            throw validation(HttpStatus.CONFLICT, "PAYMENT_NOT_SUCCESSFUL",
                    "Payment has not been completed for this ticket.");
        }
        if (!BOARDABLE_TRIP_STATUSES.contains(trip.getStatus())) {
            throw validation(HttpStatus.CONFLICT, "TRIP_NOT_BOARDABLE",
                    "This trip is not open for boarding.");
        }

        LocalDateTime now = LocalDateTime.now();
        if (ticket.getValidFrom() != null && now.isBefore(ticket.getValidFrom())) {
            throw validation(HttpStatus.CONFLICT, "NOT_YET_VALID",
                    "This ticket is not valid for boarding yet.");
        }
        if (ticket.getValidUntil() != null && now.isAfter(ticket.getValidUntil())) {
            ticket.setStatus(TicketStatus.EXPIRED);
            ticketRepository.saveAndFlush(ticket);
            throw validation(HttpStatus.CONFLICT, "EXPIRED",
                    "This ticket has expired.");
        }

        ticket.setStatus(TicketStatus.USED);
        ticket.setUsedAt(now);
        ticket.setValidatedByDriverProfile(driver);
        ticket.setValidatedTrip(trip);
        ticketRepository.save(ticket);

        return response("VALID", "Boarding confirmed.", ticket);
    }

    @Transactional
    public DriverTripManifestResponse manifest(String driverEmail, Long scheduledTripId) {
        DriverProfile driver = requireApprovedDriver(driverEmail);
        ScheduledTrip trip = scheduledTripRepository.findById(scheduledTripId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Trip not found."));
        requireAssignedDriver(driver, trip);

        List<DriverTripManifestResponse.PassengerRow> rows = ticketRepository
                .findByBookingScheduledTripAndBookingStatusOrderByBookingPassengerNameAsc(
                        trip, BookingStatus.CONFIRMED)
                .stream()
                .filter(ticket -> paymentRepository.existsByBookingAndStatus(
                        ticket.getBooking(), PaymentStatus.SUCCESS))
                .map(this::manifestRow)
                .toList();

        int boarded = (int) rows.stream()
                .filter(row -> TicketStatus.USED.name().equals(row.ticketStatus()))
                .count();

        return new DriverTripManifestResponse(
                new DriverTripManifestResponse.TripSummary(
                        tripReference(trip),
                        trip.getRoute().getOrigin(),
                        trip.getRoute().getDestination(),
                        trip.getBus().getBusName(),
                        trip.getBus().getBusNumber(),
                        trip.getDepartureAt(),
                        trip.getStatus().name()
                ),
                new DriverTripManifestResponse.BoardingSummary(
                        rows.size(), boarded, rows.size() - boarded
                ),
                rows
        );
    }

    private DriverProfile requireApprovedDriver(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Authenticated driver not found."));
        if (!"DRIVER".equalsIgnoreCase(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Driver access is required.");
        }
        DriverProfile driver = driverProfileRepository.findByUser(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Approved driver profile is required."));
        if (driver.getVerificationStatus() != DriverVerificationStatus.APPROVED
                || driver.isLicenseExpired()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Approved active driver profile is required.");
        }
        return driver;
    }

    private void requireAnyActiveAssociation(DriverProfile driver) {
        associationRepository.findByDriverAndStatus(
                driver, DriverOperatorAssociationStatus.ACTIVE
        ).orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Active operator association is required."));
    }

    private void requireAssignedDriver(DriverProfile driver, ScheduledTrip trip) {
        if (trip.getDriver() == null || !trip.getDriver().getId().equals(driver.getId())) {
            throw validation(HttpStatus.NOT_FOUND, "WRONG_TRIP",
                    "This ticket is not assigned to your trip.");
        }
        associationRepository.findByDriverAndOperator(driver, trip.getOperator())
                .filter(association -> association.getStatus() == DriverOperatorAssociationStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Active operator association is required for this trip."));
    }

    private ParsedQr parseQr(String qrPayload) {
        if (qrPayload == null || qrPayload.isBlank()) {
            throw validation(HttpStatus.BAD_REQUEST, "INVALID_QR",
                    "QR payload is required.");
        }
        if (qrPayload.length() > MAX_QR_PAYLOAD_LENGTH) {
            throw validation(HttpStatus.BAD_REQUEST, "INVALID_QR",
                    "QR payload is too large.");
        }
        try {
            JsonNode root = objectMapper.readTree(qrPayload);
            if (root.path("version").asInt(-1) != 1) {
                throw validation(HttpStatus.BAD_REQUEST, "INVALID_QR",
                        "Unsupported ticket QR version.");
            }
            String ticketNumber = text(root, "ticketNumber");
            String token = text(root, "token");
            if (ticketNumber.isBlank() || token.isBlank()) {
                throw validation(HttpStatus.BAD_REQUEST, "INVALID_QR",
                        "This QR ticket is missing required data.");
            }
            return new ParsedQr(ticketNumber, token);
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw validation(HttpStatus.BAD_REQUEST, "INVALID_QR",
                    "This QR ticket could not be read.");
        }
    }

    private String text(JsonNode root, String field) {
        JsonNode node = root.get(field);
        return node == null || node.isNull() ? "" : node.asText("").trim();
    }

    private DriverTicketValidationResponse response(String result, String message, Ticket ticket) {
        PassengerTripBooking booking = ticket.getBooking();
        ScheduledTrip trip = booking.getScheduledTrip();
        return new DriverTicketValidationResponse(
                result,
                message,
                ticket.getTicketNumber(),
                booking.getPassengerName(),
                new DriverTicketValidationResponse.RouteSummary(
                        trip.getRoute().getOrigin(),
                        trip.getRoute().getDestination()
                ),
                seatNumbers(booking),
                ticket.getUsedAt(),
                tripReference(trip)
        );
    }

    private DriverTripManifestResponse.PassengerRow manifestRow(Ticket ticket) {
        PassengerTripBooking booking = ticket.getBooking();
        return new DriverTripManifestResponse.PassengerRow(
                booking.getPassengerName(),
                booking.getPassengerPhone(),
                booking.getBookingReference(),
                ticket.getTicketNumber(),
                seatNumbers(booking),
                ticket.getStatus().name(),
                ticket.getUsedAt()
        );
    }

    private List<String> seatNumbers(PassengerTripBooking booking) {
        if (booking.getSeats() == null || booking.getSeats().isEmpty()) return List.of();
        return booking.getSeats().stream().map(BookingSeat::getSeatNumber).sorted().toList();
    }

    private String tripReference(ScheduledTrip trip) {
        return "TRIP-" + trip.getId();
    }

    private String formatUsedAt(LocalDateTime usedAt) {
        return usedAt == null ? "an earlier time" : usedAt.toString();
    }

    private TicketValidationException validation(HttpStatus status, String result, String message) {
        return new TicketValidationException(status, result + "|" + message);
    }

    private record ParsedQr(String ticketNumber, String token) {
    }

    static final class TicketValidationException extends ResponseStatusException {
        TicketValidationException(HttpStatus status, String reason) {
            super(status, reason);
        }
    }
}
