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

import java.time.LocalDateTime;
import java.util.*;

@Service
public class PassengerSeatService {
    private static final List<TripStatus> SELECTABLE = List.of(TripStatus.SCHEDULED, TripStatus.BOARDING);
    private final UserRepository userRepository;
    private final ScheduledTripRepository tripRepository;
    private final BookingSeatRepository seatRepository;
    private final long holdMinutes;
    private final int maxSeats;

    public PassengerSeatService(UserRepository userRepository, ScheduledTripRepository tripRepository,
                                BookingSeatRepository seatRepository,
                                @Value("${yatayat.booking.seat-hold-minutes:10}") long holdMinutes,
                                @Value("${yatayat.booking.max-seats:6}") int maxSeats) {
        this.userRepository = userRepository;
        this.tripRepository = tripRepository;
        this.seatRepository = seatRepository;
        this.holdMinutes = Math.max(1, holdMinutes);
        this.maxSeats = Math.max(1, maxSeats);
    }

    @Transactional
    public SeatAvailabilityResponse availability(String email, Long tripId) {
        User passenger = requirePassenger(email);
        ScheduledTrip trip = selectableTrip(tripId, false);
        LocalDateTime now = LocalDateTime.now();
        seatRepository.releaseExpired(trip, now);
        return availability(trip, passenger, now);
    }

    @Transactional
    public SeatHoldResponse hold(String email, Long tripId, SeatHoldRequest request) {
        User passenger = requirePassenger(email);
        List<String> requested = validateRequested(request);
        ScheduledTrip trip = selectableTrip(tripId, true);
        Set<String> valid = new LinkedHashSet<>(generateSeatLabels(trip.getSeatCapacitySnapshot()));
        if (!valid.containsAll(requested)) badRequest("One or more seat numbers are invalid for this bus.");

        LocalDateTime now = LocalDateTime.now();
        seatRepository.releaseExpired(trip, now);
        List<BookingSeat> previous = seatRepository
                .findByScheduledTripAndPassengerAndStatusOrderBySeatNumberAsc(
                        trip, passenger, BookingSeatStatus.HELD);
        previous.forEach(seat -> seat.release(BookingSeatStatus.RELEASED));
        seatRepository.saveAllAndFlush(previous);

        Set<String> unavailable = new HashSet<>();
        for (BookingSeat seat : seatRepository.findByScheduledTripOrderBySeatNumberAsc(trip)) {
            if (seat.getActiveSeatNumber() != null &&
                    (seat.getStatus() == BookingSeatStatus.HELD || seat.getStatus() == BookingSeatStatus.CONFIRMED)) {
                unavailable.add(seat.getSeatNumber());
            }
        }
        if (requested.stream().anyMatch(unavailable::contains)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "One or more selected seats are no longer available.");
        }

        LocalDateTime expiry = now.plusMinutes(holdMinutes);
        List<BookingSeat> holds = requested.stream().map(number -> {
            BookingSeat seat = new BookingSeat(); seat.setScheduledTrip(trip); seat.setPassenger(passenger);
            seat.setSeatNumber(number); seat.setActiveSeatNumber(number); seat.setStatus(BookingSeatStatus.HELD);
            seat.setHeldAt(now); seat.setHoldExpiresAt(expiry); return seat;
        }).toList();
        try {
            seatRepository.saveAllAndFlush(holds);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "One or more selected seats are no longer available.");
        }
        return new SeatHoldResponse(trip.getId(), requested, expiry);
    }

    @Transactional
    public void release(String email, Long tripId) {
        User passenger = requirePassenger(email);
        ScheduledTrip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found."));
        List<BookingSeat> holds = seatRepository
                .findByScheduledTripAndPassengerAndStatusOrderBySeatNumberAsc(
                        trip, passenger, BookingSeatStatus.HELD);
        holds.forEach(seat -> seat.release(BookingSeatStatus.RELEASED));
        seatRepository.saveAllAndFlush(holds);
    }

    public List<String> generateSeatLabels(int capacity) {
        List<String> labels = new ArrayList<>();
        String[] columns = {"A", "B", "C", "D"};
        for (int index = 0; index < capacity; index++) {
            labels.add((index / 4 + 1) + columns[index % 4]);
        }
        return labels;
    }

    private SeatAvailabilityResponse availability(ScheduledTrip trip, User passenger, LocalDateTime now) {
        List<String> all = generateSeatLabels(trip.getSeatCapacitySnapshot());
        Set<String> held = new LinkedHashSet<>(), confirmed = new LinkedHashSet<>(), own = new LinkedHashSet<>();
        LocalDateTime ownExpiry = null;
        for (BookingSeat seat : seatRepository.findByScheduledTripOrderBySeatNumberAsc(trip)) {
            if (seat.getStatus() == BookingSeatStatus.CONFIRMED && seat.getActiveSeatNumber() != null) {
                confirmed.add(seat.getSeatNumber());
            } else if (seat.getStatus() == BookingSeatStatus.HELD && seat.getActiveSeatNumber() != null
                    && seat.getHoldExpiresAt().isAfter(now)) {
                held.add(seat.getSeatNumber());
                if (seat.getPassenger().getId().equals(passenger.getId())) {
                    own.add(seat.getSeatNumber()); ownExpiry = seat.getHoldExpiresAt();
                }
            }
        }
        Set<String> unavailable = new HashSet<>(held); unavailable.addAll(confirmed);
        List<String> available = all.stream().filter(seat -> !unavailable.contains(seat)).toList();
        return new SeatAvailabilityResponse(trip.getId(), trip.getSeatCapacitySnapshot(), available,
                List.copyOf(held), List.copyOf(confirmed), List.copyOf(own), ownExpiry);
    }

    private List<String> validateRequested(SeatHoldRequest request) {
        if (request == null || request.seatNumbers() == null || request.seatNumbers().isEmpty())
            badRequest("Select at least one seat.");
        if (request.seatNumbers().size() > maxSeats)
            badRequest("A maximum of " + maxSeats + " seats can be held at once.");
        List<String> normalized = request.seatNumbers().stream()
                .map(value -> value == null ? "" : value.trim().toUpperCase()).toList();
        if (normalized.stream().anyMatch(String::isBlank)) badRequest("Seat numbers cannot be blank.");
        if (new HashSet<>(normalized).size() != normalized.size()) badRequest("Duplicate seat numbers are not allowed.");
        return normalized;
    }

    private ScheduledTrip selectableTrip(Long tripId, boolean lock) {
        LocalDateTime now = LocalDateTime.now();
        Optional<ScheduledTrip> visible = lock
                ? tripRepository.findPassengerVisibleByIdForUpdate(tripId, now, SELECTABLE)
                : tripRepository.findPassengerVisibleById(tripId, now, SELECTABLE);
        if (visible.isPresent()) {
            if (visible.get().getRoute().getTripType() == TripType.LOCAL)
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Local trips do not support seat selection.");
            return visible.get();
        }
        Optional<ScheduledTrip> existing = tripRepository.findById(tripId);
        if (existing.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found.");
        ScheduledTrip trip = existing.get();
        if (trip.getRoute().getTripType() == TripType.LOCAL)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Local trips do not support seat selection.");
        if (!trip.getDepartureAt().isAfter(now) || !SELECTABLE.contains(trip.getStatus()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Seat selection is not available for this trip.");
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found.");
    }

    private User requirePassenger(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user not found"));
        if (!"PASSENGER".equalsIgnoreCase(user.getRole()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Passenger access is required");
        return user;
    }
    private void badRequest(String message) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
}
