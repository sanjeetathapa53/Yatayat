package com.yatayat.backend.service;

import com.yatayat.backend.dto.*;
import com.yatayat.backend.entity.*;
import com.yatayat.backend.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
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
public class PassengerBookingService {
    private static final List<TripStatus> BOOKABLE_STATUSES =
            List.of(TripStatus.SCHEDULED, TripStatus.BOARDING);
    private static final DateTimeFormatter REFERENCE_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final UserRepository userRepository;
    private final ScheduledTripRepository tripRepository;
    private final PassengerTripBookingRepository bookingRepository;
    private final BookingSeatRepository seatRepository;
    private final long paymentWindowMinutes;

    public PassengerBookingService(UserRepository userRepository,
                                   ScheduledTripRepository tripRepository,
                                   PassengerTripBookingRepository bookingRepository,
                                   BookingSeatRepository seatRepository,
                                   @Value("${yatayat.booking.payment-window-minutes:10}") long paymentWindowMinutes) {
        this.userRepository = userRepository;
        this.tripRepository = tripRepository;
        this.bookingRepository = bookingRepository;
        this.seatRepository = seatRepository;
        this.paymentWindowMinutes = Math.max(1, paymentWindowMinutes);
    }

    @Transactional
    public PassengerBookingDetailsResponse create(String email, CreatePassengerBookingRequest request) {
        User passenger = requirePassenger(email);
        validateRequest(request);
        ScheduledTrip trip = tripRepository.findPassengerVisibleByIdForUpdate(
                        request.tripId(), LocalDateTime.now(), BOOKABLE_STATUSES)
                .filter(this::resourcesValidForDeparture)
                .orElseThrow(this::tripNotFound);

        if (trip.getRoute().getTripType() == TripType.LOCAL) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Local trips do not support seat reservations.");
        }

        LocalDateTime now = LocalDateTime.now();
        seatRepository.releaseExpired(trip, now);
        List<String> requestedSeats = request.seatNumbers().stream()
                .map(value -> value.trim().toUpperCase()).sorted().toList();
        List<BookingSeat> holds = seatRepository
                .findByScheduledTripAndPassengerAndStatusOrderBySeatNumberAsc(
                        trip, passenger, BookingSeatStatus.HELD).stream()
                .filter(seat -> seat.getActiveSeatNumber() != null && seat.getHoldExpiresAt().isAfter(now))
                .toList();
        List<String> heldSeats = holds.stream().map(BookingSeat::getSeatNumber).sorted().toList();
        if (!heldSeats.equals(requestedSeats)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "An active seat hold is required for every selected seat.");
        }

        PassengerTripBooking booking = new PassengerTripBooking();
        booking.setBookingReference(generateReference());
        booking.setPassenger(passenger);
        booking.setScheduledTrip(trip);
        booking.setPassengerName(request.passengerName());
        booking.setPassengerPhone(request.passengerPhone());
        booking.setNumberOfSeats(requestedSeats.size());
        booking.setFarePerSeat(trip.getFare());
        booking.setTotalFare(trip.getFare().multiply(BigDecimal.valueOf(requestedSeats.size())));
        booking.setStatus(BookingStatus.PENDING_PAYMENT);
        LocalDateTime paymentExpiry = now.plusMinutes(paymentWindowMinutes);
        holds.forEach(seat -> { seat.setBooking(booking); seat.setHoldExpiresAt(paymentExpiry); });
        booking.setSeats(holds);
        try {
            return toDetails(bookingRepository.saveAndFlush(booking));
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Booking could not be created. Please try again.");
        }
    }

    public List<PassengerBookingSummaryResponse> list(String email) {
        User passenger = requirePassenger(email);
        return bookingRepository.findByPassengerOrderByBookedAtDesc(passenger)
                .stream().map(this::toSummary).toList();
    }

    public PassengerBookingDetailsResponse details(String email, String reference) {
        User passenger = requirePassenger(email);
        return toDetails(ownedBooking(passenger, reference));
    }

    @Transactional
    public PassengerBookingDetailsResponse cancel(String email, String reference) {
        User passenger = requirePassenger(email);
        PassengerTripBooking booking = ownedBooking(passenger, reference);
        if (booking.getStatus() == BookingStatus.CANCELLED) return toDetails(booking);

        ScheduledTrip trip = booking.getScheduledTrip();
        if (!trip.getDepartureAt().isAfter(LocalDateTime.now())
                || trip.getStatus() == TripStatus.IN_PROGRESS
                || trip.getStatus() == TripStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This booking can no longer be cancelled.");
        }
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(LocalDateTime.now());
        List<BookingSeat> seats = seatRepository.findByBookingOrderBySeatNumberAsc(booking);
        seats.stream().filter(seat -> seat.getActiveSeatNumber() != null)
                .forEach(seat -> seat.release(BookingSeatStatus.CANCELLED));
        seatRepository.saveAll(seats);
        return toDetails(bookingRepository.saveAndFlush(booking));
    }

    private PassengerTripBooking ownedBooking(User passenger, String reference) {
        if (reference == null || reference.isBlank()) throw bookingNotFound();
        return bookingRepository.findByBookingReferenceAndPassenger(reference.trim(), passenger)
                .orElseThrow(this::bookingNotFound);
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

    private void validateRequest(CreatePassengerBookingRequest request) {
        if (request == null || request.tripId() == null) badRequest("Trip is required.");
        if (request.seatNumbers() == null || request.seatNumbers().isEmpty())
            badRequest("Selected seats are required.");
        if (request.seatNumbers().size() > 6) badRequest("A maximum of 6 seats can be booked at once.");
        List<String> normalized = request.seatNumbers().stream()
                .map(value -> value == null ? "" : value.trim().toUpperCase()).toList();
        if (normalized.stream().anyMatch(String::isBlank)) badRequest("Seat numbers cannot be blank.");
        if (new java.util.HashSet<>(normalized).size() != normalized.size())
            badRequest("Duplicate seat numbers are not allowed.");
        if (request.numberOfSeats() != null && request.numberOfSeats() != normalized.size())
            badRequest("Passenger quantity must match the selected seats.");
        String name = request.passengerName();
        if (name == null || name.trim().length() < 2 || name.trim().length() > 120)
            badRequest("Passenger name must be between 2 and 120 characters.");
        String phone = request.passengerPhone();
        if (phone == null || !phone.trim().matches("^[0-9+()\\-\\s]{7,20}$"))
            badRequest("Passenger phone is invalid.");
    }

    private boolean resourcesValidForDeparture(ScheduledTrip trip) {
        LocalDate date = trip.getDepartureAt().toLocalDate();
        return BOOKABLE_STATUSES.contains(trip.getStatus())
                && trip.getDepartureAt().isAfter(LocalDateTime.now())
                && trip.getRoute().getStatus() == RouteStatus.ACTIVE
                && trip.getOperator().isApproved()
                && trip.getBus().getStatus() == BusStatus.APPROVED
                && trip.getDriver().isApproved()
                && trip.getBus().getPermitExpiryDate() != null
                && !trip.getBus().getPermitExpiryDate().isBefore(date)
                && trip.getBus().getInsuranceExpiryDate() != null
                && !trip.getBus().getInsuranceExpiryDate().isBefore(date)
                && trip.getDriver().getLicenseExpiryDate() != null
                && !trip.getDriver().getLicenseExpiryDate().isBefore(date);
    }

    private String generateReference() {
        for (int attempt = 0; attempt < 8; attempt++) {
            String suffix = UUID.randomUUID().toString().replace("-", "")
                    .substring(0, 6).toUpperCase();
            String reference = "YAT-" + LocalDate.now().format(REFERENCE_DATE) + "-" + suffix;
            if (!bookingRepository.existsByBookingReference(reference)) return reference;
        }
        throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Booking reference could not be generated. Please try again.");
    }

    private PassengerBookingSummaryResponse toSummary(PassengerTripBooking booking) {
        ScheduledTrip trip = booking.getScheduledTrip();
        return new PassengerBookingSummaryResponse(
                booking.getBookingReference(), booking.getStatus().name(), trip.getId(),
                trip.getRoute().getCode(), trip.getRoute().getName(),
                trip.getRoute().getTripType().name(), trip.getRoute().getOrigin(),
                trip.getRoute().getDestination(), trip.getDepartureAt(), trip.getEstimatedArrivalAt(),
                trip.getOperator().getName(), trip.getBus().getBusNumber(), booking.getNumberOfSeats(),
                seatNumbers(booking),
                booking.getFarePerSeat(), booking.getTotalFare(), booking.getBookedAt(), booking.getCancelledAt()
        );
    }

    private PassengerBookingDetailsResponse toDetails(PassengerTripBooking booking) {
        PassengerBookingSummaryResponse summary = toSummary(booking);
        return new PassengerBookingDetailsResponse(
                summary.bookingReference(), summary.bookingStatus(), booking.getPassengerName(),
                maskPhone(booking.getPassengerPhone()), summary.tripId(), summary.routeCode(),
                summary.routeName(), summary.tripType(), summary.origin(), summary.destination(), summary.operatorName(),
                summary.busNumber(), summary.departureAt(), summary.estimatedArrivalAt(),
                summary.numberOfSeats(), summary.seatNumbers(), summary.farePerSeat(), summary.totalFare(),
                summary.bookedAt(), summary.cancelledAt(), booking.getScheduledTrip().getBoardingNotes()
        );
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() <= 4) return phone;
        return "*".repeat(phone.length() - 4) + phone.substring(phone.length() - 4);
    }
    private List<String> seatNumbers(PassengerTripBooking booking) {
        if (booking.getSeats() == null || booking.getSeats().isEmpty()) return List.of();
        return booking.getSeats().stream().map(BookingSeat::getSeatNumber).sorted().toList();
    }
    private void badRequest(String message) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private ResponseStatusException tripNotFound() { return new ResponseStatusException(HttpStatus.NOT_FOUND, "This trip is no longer available for booking."); }
    private ResponseStatusException bookingNotFound() { return new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found."); }
}
