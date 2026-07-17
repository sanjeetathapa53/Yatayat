package com.yatayat.backend.service;

import com.yatayat.backend.dto.PassengerTripDetailsResponse;
import com.yatayat.backend.dto.PassengerTripSearchResponse;
import com.yatayat.backend.entity.ScheduledTrip;
import com.yatayat.backend.entity.TripStatus;
import com.yatayat.backend.entity.User;
import com.yatayat.backend.repository.ScheduledTripRepository;
import com.yatayat.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PassengerTripService {
    private static final List<TripStatus> VISIBLE_STATUSES =
            List.of(TripStatus.SCHEDULED, TripStatus.BOARDING);

    private final UserRepository userRepository;
    private final ScheduledTripRepository tripRepository;

    public PassengerTripService(UserRepository userRepository, ScheduledTripRepository tripRepository) {
        this.userRepository = userRepository;
        this.tripRepository = tripRepository;
    }

    public List<PassengerTripSearchResponse> search(
            String email, String origin, String destination, LocalDate date
    ) {
        requirePassenger(email);
        String cleanOrigin = requiredSearchValue(origin, "Origin is required");
        String cleanDestination = requiredSearchValue(destination, "Destination is required");
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from = date == null ? null : date.atStartOfDay();
        LocalDateTime to = date == null ? null : date.plusDays(1).atStartOfDay();

        return tripRepository.searchPassengerVisible(
                        cleanOrigin, cleanDestination, now, VISIBLE_STATUSES, from, to
                ).stream()
                .filter(this::resourcesValidForDeparture)
                .map(this::toSearchResponse)
                .toList();
    }

    public PassengerTripDetailsResponse details(String email, Long tripId) {
        requirePassenger(email);
        if (tripId == null) throw notFound();
        ScheduledTrip trip = tripRepository.findPassengerVisibleById(
                        tripId, LocalDateTime.now(), VISIBLE_STATUSES)
                .filter(this::resourcesValidForDeparture)
                .orElseThrow(this::notFound);
        return toDetailsResponse(trip);
    }

    private void requirePassenger(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Authenticated user not found"));
        if (!"PASSENGER".equalsIgnoreCase(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Passenger access is required");
        }
    }

    private boolean resourcesValidForDeparture(ScheduledTrip trip) {
        LocalDate tripDate = trip.getDepartureAt().toLocalDate();
        return VISIBLE_STATUSES.contains(trip.getStatus())
                && trip.getDepartureAt().isAfter(LocalDateTime.now())
                && trip.getRoute().getStatus() == com.yatayat.backend.entity.RouteStatus.ACTIVE
                && trip.getOperator().isApproved()
                && trip.getBus().getStatus() == com.yatayat.backend.entity.BusStatus.APPROVED
                && trip.getDriver().isApproved()
                && trip.getBus().getPermitExpiryDate() != null
                && !trip.getBus().getPermitExpiryDate().isBefore(tripDate)
                && trip.getBus().getInsuranceExpiryDate() != null
                && !trip.getBus().getInsuranceExpiryDate().isBefore(tripDate)
                && trip.getDriver().getLicenseExpiryDate() != null
                && !trip.getDriver().getLicenseExpiryDate().isBefore(tripDate);
    }

    private PassengerTripSearchResponse toSearchResponse(ScheduledTrip trip) {
        return new PassengerTripSearchResponse(
                trip.getId(), trip.getRoute().getId(), trip.getRoute().getCode(),
                trip.getRoute().getName(), trip.getRoute().getOrigin(),
                trip.getRoute().getDestination(), trip.getDepartureAt(),
                trip.getEstimatedArrivalAt(), trip.getFare(),
                trip.getSeatCapacitySnapshot(), trip.getStatus().name(),
                trip.getOperator().getName(), trip.getBus().getBusNumber(),
                trip.getBus().getBusName(), trip.getRoute().getEstimatedDurationMinutes(),
                trip.getRoute().getTripType().name()
        );
    }

    private PassengerTripDetailsResponse toDetailsResponse(ScheduledTrip trip) {
        PassengerTripSearchResponse summary = toSearchResponse(trip);
        return new PassengerTripDetailsResponse(
                summary.tripId(), summary.routeId(), summary.routeCode(),
                summary.routeName(), summary.origin(), summary.destination(),
                summary.departureAt(), summary.estimatedArrivalAt(), summary.fare(),
                summary.seatCapacity(), summary.status(), summary.operatorName(),
                summary.busNumber(), summary.busName(), summary.estimatedDurationMinutes(),
                summary.tripType(),
                trip.getBoardingNotes()
        );
    }

    private String requiredSearchValue(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return value.trim();
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Scheduled trip not found");
    }
}
