package com.yatayat.backend.service;

import com.yatayat.backend.dto.*;
import com.yatayat.backend.entity.*;
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
public class PassengerBookingService {
    private static final List<TripStatus> BOOKABLE_STATUSES =
            List.of(TripStatus.SCHEDULED, TripStatus.BOARDING);
    private static final DateTimeFormatter REFERENCE_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final UserRepository userRepository;
    private final ScheduledTripRepository tripRepository;
    private final PassengerTripBookingRepository bookingRepository;

    public PassengerBookingService(UserRepository userRepository,
                                   ScheduledTripRepository tripRepository,
                                   PassengerTripBookingRepository bookingRepository) {
        this.userRepository = userRepository;
        this.tripRepository = tripRepository;
        this.bookingRepository = bookingRepository;
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

        long confirmedSeats = bookingRepository.sumConfirmedSeatsByTrip(trip);
        long remaining = (long) trip.getSeatCapacitySnapshot() - confirmedSeats;
        if (request.numberOfSeats() > remaining) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Not enough seats are available for this trip.");
        }

        PassengerTripBooking booking = new PassengerTripBooking();
        booking.setBookingReference(generateReference());
        booking.setPassenger(passenger);
        booking.setScheduledTrip(trip);
        booking.setPassengerName(request.passengerName());
        booking.setPassengerPhone(request.passengerPhone());
        booking.setNumberOfSeats(request.numberOfSeats());
        booking.setFarePerSeat(trip.getFare());
        booking.setTotalFare(trip.getFare().multiply(BigDecimal.valueOf(request.numberOfSeats())));
        booking.setStatus(BookingStatus.CONFIRMED);
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
        if (request.numberOfSeats() == null || request.numberOfSeats() <= 0)
            badRequest("Number of seats must be greater than zero.");
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
                summary.numberOfSeats(), summary.farePerSeat(), summary.totalFare(),
                summary.bookedAt(), summary.cancelledAt(), booking.getScheduledTrip().getBoardingNotes()
        );
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() <= 4) return phone;
        return "*".repeat(phone.length() - 4) + phone.substring(phone.length() - 4);
    }
    private void badRequest(String message) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private ResponseStatusException tripNotFound() { return new ResponseStatusException(HttpStatus.NOT_FOUND, "This trip is no longer available for booking."); }
    private ResponseStatusException bookingNotFound() { return new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found."); }
}
