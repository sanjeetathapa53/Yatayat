package com.yatayat.backend.trip;

import com.yatayat.backend.dto.SeatHoldRequest;
import com.yatayat.backend.entity.*;
import com.yatayat.backend.repository.BookingSeatRepository;
import com.yatayat.backend.repository.ScheduledTripRepository;
import com.yatayat.backend.repository.UserRepository;
import com.yatayat.backend.service.PassengerSeatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PassengerSeatServiceTests {
    private UserRepository users; private ScheduledTripRepository trips; private BookingSeatRepository seats;
    private PassengerSeatService service; private User passenger; private ScheduledTrip trip;

    @BeforeEach void setUp() {
        users = mock(UserRepository.class); trips = mock(ScheduledTripRepository.class); seats = mock(BookingSeatRepository.class);
        service = new PassengerSeatService(users, trips, seats, 10, 6);
        passenger = new User(); passenger.setId(7L); passenger.setEmail("passenger@test.local"); passenger.setRole("PASSENGER");
        Route route = new Route(); route.setTripType(TripType.OUT_OF_VALLEY);
        trip = new ScheduledTrip(); trip.setId(11L); trip.setRoute(route); trip.setSeatCapacitySnapshot(5);
        trip.setDepartureAt(LocalDateTime.now().plusDays(1)); trip.setStatus(TripStatus.SCHEDULED);
        when(users.findByEmailIgnoreCase(passenger.getEmail())).thenReturn(Optional.of(passenger));
        when(trips.findPassengerVisibleByIdForUpdate(eq(11L), any(), anyList())).thenReturn(Optional.of(trip));
        when(trips.findPassengerVisibleById(eq(11L), any(), anyList())).thenReturn(Optional.of(trip));
        when(seats.findByScheduledTripAndPassengerAndStatusOrderBySeatNumberAsc(trip, passenger, BookingSeatStatus.HELD)).thenReturn(List.of());
        when(seats.findByScheduledTripOrderBySeatNumberAsc(trip)).thenReturn(List.of());
    }

    @Test void labelsRespectExactBusCapacity() { assertThat(service.generateSeatLabels(5)).containsExactly("1A", "1B", "1C", "1D", "2A"); }

    @Test void duplicateSelectionIsRejected() {
        assertThatThrownBy(() -> service.hold(passenger.getEmail(), 11L, new SeatHoldRequest(List.of("1A", "1a"))))
                .isInstanceOfSatisfying(ResponseStatusException.class, error -> assertThat(error.getStatusCode().value()).isEqualTo(400));
    }

    @Test void localTripIsRejected() {
        trip.getRoute().setTripType(TripType.LOCAL);
        assertThatThrownBy(() -> service.hold(passenger.getEmail(), 11L, new SeatHoldRequest(List.of("1A"))))
                .isInstanceOfSatisfying(ResponseStatusException.class, error -> assertThat(error.getStatusCode().value()).isEqualTo(409));
    }

    @Test void databaseConflictBecomesSeatConflict() {
        when(seats.saveAllAndFlush(anyList())).thenReturn(List.of())
                .thenThrow(new DataIntegrityViolationException("unique active seat"));
        assertThatThrownBy(() -> service.hold(passenger.getEmail(), 11L, new SeatHoldRequest(List.of("1A"))))
                .isInstanceOfSatisfying(ResponseStatusException.class, error -> {
                    assertThat(error.getStatusCode().value()).isEqualTo(409);
                    assertThat(error.getReason()).contains("no longer available");
                });
    }

    @Test void passengerCanReleaseOnlyOwnHolds() {
        BookingSeat own = new BookingSeat(); own.setSeatNumber("1A"); own.setActiveSeatNumber("1A"); own.setStatus(BookingSeatStatus.HELD);
        when(trips.findById(11L)).thenReturn(Optional.of(trip));
        when(seats.findByScheduledTripAndPassengerAndStatusOrderBySeatNumberAsc(trip, passenger, BookingSeatStatus.HELD)).thenReturn(List.of(own));
        service.release(passenger.getEmail(), 11L);
        assertThat(own.getStatus()).isEqualTo(BookingSeatStatus.RELEASED); assertThat(own.getActiveSeatNumber()).isNull();
        verify(seats).saveAllAndFlush(List.of(own));
    }
}
