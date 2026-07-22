package com.yatayat.backend.trip;

import com.yatayat.backend.dto.TripLocationResponse;
import com.yatayat.backend.dto.TripLocationUpdateRequest;
import com.yatayat.backend.entity.*;
import com.yatayat.backend.repository.*;
import com.yatayat.backend.service.TripLocationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TripLocationServiceTests {

    @Mock private UserRepository userRepository;
    @Mock private DriverProfileRepository driverRepository;
    @Mock private ScheduledTripRepository tripRepository;
    @Mock private TripLocationRepository locationRepository;

    private TripLocationService service;
    private User driverUser;
    private DriverProfile driver;
    private ScheduledTrip trip;

    @BeforeEach
    void setUp() {
        service = new TripLocationService(
                userRepository, driverRepository, tripRepository, locationRepository);

        driverUser = new User("Driver", "driver@example.com", "9800000000", "encoded", "DRIVER");
        driverUser.setId(1L);
        driver = new DriverProfile(driverUser);
        driver.setId(2L);
        driver.setVerificationStatus(DriverVerificationStatus.APPROVED);
        driver.setLicenseExpiryDate(LocalDate.now().plusYears(1));

        trip = new ScheduledTrip();
        trip.setId(50L);
        trip.setDriver(driver);
        trip.setStatus(TripStatus.IN_PROGRESS);
    }

    @Test
    void validUpdateStoresAndReturnsLatestLocation() {
        mockApprovedDriver();
        when(tripRepository.findByIdForOperation(50L)).thenReturn(Optional.of(trip));
        when(locationRepository.findByTrip(trip)).thenReturn(Optional.empty());
        when(locationRepository.saveAndFlush(any(TripLocation.class))).thenAnswer(invocation -> {
            TripLocation location = invocation.getArgument(0);
            location.setId(10L);
            location.setUpdatedAt(LocalDateTime.now());
            return location;
        });

        TripLocationResponse response = service.update(
                "driver@example.com", 50L,
                new TripLocationUpdateRequest(27.7172, 85.3240, 8.0, 32.5, 180.0));

        assertThat(response.tripId()).isEqualTo(50L);
        assertThat(response.latitude()).isEqualTo(27.7172);
        assertThat(response.longitude()).isEqualTo(85.3240);
        assertThat(response.accuracy()).isEqualTo(8.0);
        assertThat(response.speed()).isEqualTo(32.5);
        assertThat(response.heading()).isEqualTo(180.0);
        assertThat(response.updatedAt()).isNotNull();
    }

    @Test
    void unapprovedDriverIsForbidden() {
        when(userRepository.findByEmailIgnoreCase("driver@example.com"))
                .thenReturn(Optional.of(driverUser));
        driver.setVerificationStatus(DriverVerificationStatus.PENDING);
        when(driverRepository.findByUser(driverUser)).thenReturn(Optional.of(driver));

        assertStatus(
                () -> service.update("driver@example.com", 50L, validRequest()),
                HttpStatus.FORBIDDEN
        );
        verifyNoInteractions(tripRepository, locationRepository);
    }

    @Test
    void driverCannotUpdateAnotherDriversTrip() {
        mockApprovedDriver();
        DriverProfile other = new DriverProfile(
                new User("Other", "other@example.com", "", "", "DRIVER"));
        other.setId(99L);
        trip.setDriver(other);
        when(tripRepository.findByIdForOperation(50L)).thenReturn(Optional.of(trip));

        assertStatus(
                () -> service.update("driver@example.com", 50L, validRequest()),
                HttpStatus.NOT_FOUND
        );
        verifyNoInteractions(locationRepository);
    }

    @Test
    void inactiveTripIsRejected() {
        mockApprovedDriver();
        trip.setStatus(TripStatus.COMPLETED);
        when(tripRepository.findByIdForOperation(50L)).thenReturn(Optional.of(trip));

        assertStatus(
                () -> service.update("driver@example.com", 50L, validRequest()),
                HttpStatus.BAD_REQUEST
        );
        verifyNoInteractions(locationRepository);
    }

    @Test
    void secondUpdateReusesExistingLocationRecord() {
        mockApprovedDriver();
        TripLocation existing = new TripLocation();
        existing.setId(10L);
        existing.setTrip(trip);
        existing.setLatitude(27.0);
        existing.setLongitude(85.0);

        when(tripRepository.findByIdForOperation(50L)).thenReturn(Optional.of(trip));
        when(locationRepository.findByTrip(trip)).thenReturn(Optional.of(existing));
        when(locationRepository.saveAndFlush(existing)).thenAnswer(invocation -> {
            existing.setUpdatedAt(LocalDateTime.now());
            return existing;
        });

        TripLocationResponse response = service.update(
                "driver@example.com", 50L,
                new TripLocationUpdateRequest(28.1, 84.2, null, 40.0, 90.0));

        assertThat(response.latitude()).isEqualTo(28.1);
        assertThat(response.longitude()).isEqualTo(84.2);
        assertThat(existing.getId()).isEqualTo(10L);
        verify(locationRepository).saveAndFlush(existing);
    }

    private TripLocationUpdateRequest validRequest() {
        return new TripLocationUpdateRequest(27.7, 85.3, null, null, null);
    }

    private void mockApprovedDriver() {
        when(userRepository.findByEmailIgnoreCase("driver@example.com"))
                .thenReturn(Optional.of(driverUser));
        when(driverRepository.findByUser(driverUser)).thenReturn(Optional.of(driver));
    }

    private void assertStatus(Runnable operation, HttpStatus status) {
        assertThatThrownBy(operation::run)
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(status);
    }
}
