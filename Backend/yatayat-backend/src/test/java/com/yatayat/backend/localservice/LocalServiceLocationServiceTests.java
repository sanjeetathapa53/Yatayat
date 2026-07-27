package com.yatayat.backend.localservice;

import com.yatayat.backend.dto.LocalServiceLocationResponse;
import com.yatayat.backend.dto.TripLocationUpdateRequest;
import com.yatayat.backend.entity.*;
import com.yatayat.backend.repository.*;
import com.yatayat.backend.service.LocalServiceLocationService;
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
class LocalServiceLocationServiceTests {
    @Mock private UserRepository userRepository;
    @Mock private DriverProfileRepository driverRepository;
    @Mock private DriverOperatorAssociationRepository associationRepository;
    @Mock private LocalServiceRunRepository runRepository;
    @Mock private LocalServiceLocationRepository locationRepository;

    private LocalServiceLocationService service;
    private User user;
    private DriverProfile driver;
    private LocalServiceRun run;

    @BeforeEach
    void setUp() {
        service = new LocalServiceLocationService(
                userRepository, driverRepository, associationRepository,
                runRepository, locationRepository);
        user = new User("Driver", "driver@example.com", "9800000000", "encoded", "DRIVER");
        driver = new DriverProfile(user);
        driver.setId(2L);
        driver.setVerificationStatus(DriverVerificationStatus.APPROVED);
        driver.setLicenseExpiryDate(LocalDate.now().plusYears(1));

        TransportOperator operator = new TransportOperator();
        operator.setId(3L);
        run = new LocalServiceRun();
        run.setId(10L);
        run.setDriver(driver);
        run.setOperator(operator);
        run.setStatus(LocalServiceRunStatus.IN_SERVICE);
    }

    @Test
    void validUpdateStoresLatestLocation() {
        prepareAuthorizedDriver();
        when(locationRepository.findByRun(run)).thenReturn(Optional.empty());
        when(locationRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            LocalServiceLocation location = invocation.getArgument(0);
            location.setUpdatedAt(LocalDateTime.now());
            return location;
        });

        LocalServiceLocationResponse response = service.update(
                user.getEmail(), 10L,
                new TripLocationUpdateRequest(27.7, 85.3, 7.0, 8.0, 90.0));

        assertThat(response.runId()).isEqualTo(10L);
        assertThat(response.latitude()).isEqualTo(27.7);
        assertThat(response.longitude()).isEqualTo(85.3);
        assertThat(response.updatedAt()).isNotNull();
    }

    @Test
    void anotherDriversRunIsNotFound() {
        mockApprovedDriver();
        when(runRepository.findByIdAndDriverForOperation(10L, driver)).thenReturn(Optional.empty());

        assertStatus(() -> service.update(user.getEmail(), 10L, validRequest()), HttpStatus.NOT_FOUND);
        verifyNoInteractions(locationRepository);
    }

    @Test
    void inactiveServiceRejectsLocation() {
        prepareAuthorizedDriver();
        run.setStatus(LocalServiceRunStatus.COMPLETED);

        assertStatus(() -> service.update(user.getEmail(), 10L, validRequest()), HttpStatus.BAD_REQUEST);
        verifyNoInteractions(locationRepository);
    }

    @Test
    void inactiveOperatorAssociationIsForbidden() {
        mockApprovedDriver();
        when(runRepository.findByIdAndDriverForOperation(10L, driver)).thenReturn(Optional.of(run));
        DriverOperatorAssociation removed = new DriverOperatorAssociation();
        removed.setStatus(DriverOperatorAssociationStatus.REMOVED);
        when(associationRepository.findByDriverAndOperator(driver, run.getOperator()))
                .thenReturn(Optional.of(removed));

        assertStatus(() -> service.update(user.getEmail(), 10L, validRequest()), HttpStatus.FORBIDDEN);
        verifyNoInteractions(locationRepository);
    }

    @Test
    void secondUpdateReusesExistingRecord() {
        prepareAuthorizedDriver();
        LocalServiceLocation existing = new LocalServiceLocation();
        existing.setRun(run);
        existing.setLatitude(27.0);
        existing.setLongitude(85.0);
        when(locationRepository.findByRun(run)).thenReturn(Optional.of(existing));
        when(locationRepository.saveAndFlush(existing)).thenAnswer(invocation -> {
            existing.setUpdatedAt(LocalDateTime.now());
            return existing;
        });

        LocalServiceLocationResponse response = service.update(
                user.getEmail(), 10L,
                new TripLocationUpdateRequest(28.0, 84.0, null, null, null));

        assertThat(response.latitude()).isEqualTo(28.0);
        assertThat(response.longitude()).isEqualTo(84.0);
        verify(locationRepository).saveAndFlush(existing);
    }

    private void prepareAuthorizedDriver() {
        mockApprovedDriver();
        when(runRepository.findByIdAndDriverForOperation(10L, driver)).thenReturn(Optional.of(run));
        DriverOperatorAssociation active = new DriverOperatorAssociation();
        active.setStatus(DriverOperatorAssociationStatus.ACTIVE);
        when(associationRepository.findByDriverAndOperator(driver, run.getOperator()))
                .thenReturn(Optional.of(active));
    }

    private void mockApprovedDriver() {
        when(userRepository.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
        when(driverRepository.findByUser(user)).thenReturn(Optional.of(driver));
    }

    private TripLocationUpdateRequest validRequest() {
        return new TripLocationUpdateRequest(27.7, 85.3, null, null, null);
    }

    private void assertStatus(Runnable operation, HttpStatus status) {
        assertThatThrownBy(operation::run)
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(status);
    }
}
