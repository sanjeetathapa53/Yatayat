package com.yatayat.backend.localfare;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yatayat.backend.entity.*;
import com.yatayat.backend.repository.*;
import com.yatayat.backend.service.DriverLocalFarePassValidationService;
import com.yatayat.backend.service.LocalFarePassQrTokenService;
import com.yatayat.backend.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DriverLocalFarePassValidationServiceTests {
    @Mock UserRepository userRepository;
    @Mock DriverProfileRepository driverProfileRepository;
    @Mock DriverOperatorAssociationRepository associationRepository;
    @Mock LocalServiceRunRepository runRepository;
    @Mock LocalFarePassRepository passRepository;
    @Mock NotificationService notificationService;

    DriverLocalFarePassValidationService service;
    LocalFarePassQrTokenService tokens;
    User driverUser;
    DriverProfile driver;
    TransportOperator operator;
    Route route;
    LocalServiceRun run;
    LocalFarePass pass;
    DriverOperatorAssociation association;

    @BeforeEach
    void setUp() {
        tokens = new LocalFarePassQrTokenService(
                "test-only-local-fare-pass-secret-at-least-32-characters");
        service = new DriverLocalFarePassValidationService(
                new ObjectMapper(), userRepository, driverProfileRepository,
                associationRepository, runRepository, passRepository, tokens, notificationService);
        driverUser = new User("Driver", "driver@example.com", "9800000001", "encoded", "DRIVER");
        driverUser.setId(1L);
        driver = new DriverProfile(driverUser);
        driver.setId(2L);
        driver.setVerificationStatus(DriverVerificationStatus.APPROVED);
        driver.setLicenseExpiryDate(LocalDate.now().plusYears(1));
        operator = new TransportOperator();
        operator.setId(3L);
        route = new Route();
        route.setId(4L);
        route.setCode("L-4");
        route.setName("Local Route");
        run = new LocalServiceRun();
        run.setId(5L);
        run.setDriver(driver);
        run.setOperator(operator);
        run.setRoute(route);
        run.setStatus(LocalServiceRunStatus.IN_SERVICE);
        association = new DriverOperatorAssociation();
        association.setDriver(driver);
        association.setOperator(operator);
        association.setStatus(DriverOperatorAssociationStatus.ACTIVE);
        pass = pass();
        lenient().when(userRepository.findByEmailIgnoreCase(driverUser.getEmail()))
                .thenReturn(Optional.of(driverUser));
        lenient().when(driverProfileRepository.findByUser(driverUser)).thenReturn(Optional.of(driver));
    }

    @Test
    void validPassIsUsedExactlyOnceWithAudit() {
        mockValid();
        var response = service.validate(driverUser.getEmail(), payload());
        assertThat(response.result()).isEqualTo("VALID");
        assertThat(pass.getStatus()).isEqualTo(LocalFarePassStatus.USED);
        assertThat(pass.getValidatedByDriverProfile()).isSameAs(driver);
        assertThat(pass.getValidatedLocalServiceRun()).isSameAs(run);
        verify(passRepository).save(pass);
        verify(notificationService).localFarePassUsed(pass);
    }

    @Test
    void duplicateScanPreservesOriginalAudit() {
        mockValid();
        LocalDateTime original = LocalDateTime.now().minusMinutes(2);
        pass.setStatus(LocalFarePassStatus.USED);
        pass.setUsedAt(original);
        pass.setValidatedByDriverProfile(driver);
        pass.setValidatedLocalServiceRun(run);
        assertThatThrownBy(() -> service.validate(driverUser.getEmail(), payload()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("ALREADY_USED");
        assertThat(pass.getUsedAt()).isEqualTo(original);
        assertThat(pass.getValidatedLocalServiceRun()).isSameAs(run);
        verify(passRepository, never()).save(pass);
        verify(notificationService, never()).localFarePassUsed(any());
    }

    @Test
    void expiredPassIsPersistedAndRejected() {
        mockValid();
        pass.setValidUntil(LocalDateTime.now().minusSeconds(1));
        assertThatThrownBy(() -> service.validate(driverUser.getEmail(), payload()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("EXPIRED");
        assertThat(pass.getStatus()).isEqualTo(LocalFarePassStatus.EXPIRED);
        verify(passRepository).saveAndFlush(pass);
    }

    @Test
    void activeRunMustMatchPassRoute() {
        mockValid();
        Route other = new Route();
        other.setId(99L);
        run.setRoute(other);
        assertThatThrownBy(() -> service.validate(driverUser.getEmail(), payload()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("WRONG_ROUTE");
    }

    @Test
    void malformedUnsupportedAndTamperedPayloadsAreRejected() {
        assertThatThrownBy(() -> service.validate(driverUser.getEmail(), "not-json"))
                .hasMessageContaining("INVALID_QR");
        assertThatThrownBy(() -> service.validate(driverUser.getEmail(),
                "{\"version\":2,\"type\":\"LOCAL_FARE_PASS\",\"passNumber\":\"x\",\"token\":\"x\"}"))
                .hasMessageContaining("INVALID_QR");
        when(passRepository.findByPassNumberForValidation(pass.getPassNumber()))
                .thenReturn(Optional.of(pass));
        assertThatThrownBy(() -> service.validate(driverUser.getEmail(),
                "{\"version\":1,\"type\":\"LOCAL_FARE_PASS\",\"passNumber\":\""
                        + pass.getPassNumber() + "\",\"token\":\"altered\"}"))
                .hasMessageContaining("INVALID_QR");
    }

    private void mockValid() {
        when(passRepository.findByPassNumberForValidation(pass.getPassNumber()))
                .thenReturn(Optional.of(pass));
        lenient().when(runRepository.findByDriverAndStatusOrderByActualStartedAtDesc(
                driver, LocalServiceRunStatus.IN_SERVICE)).thenReturn(List.of(run));
        lenient().when(associationRepository.findByDriverAndOperator(driver, operator))
                .thenReturn(Optional.of(association));
    }

    private String payload() {
        return "{\"version\":1,\"type\":\"LOCAL_FARE_PASS\",\"passNumber\":\""
                + pass.getPassNumber() + "\",\"token\":\""
                + tokens.rawToken(pass.getPassNumber()) + "\"}";
    }

    private LocalFarePass pass() {
        User passenger = new User("Passenger", "passenger@example.com", "9800000002", "encoded", "PASSENGER");
        LocalFarePass value = new LocalFarePass();
        value.setId(10L);
        value.setPassNumber("YT-LFP-TEST-1");
        value.setPassenger(passenger);
        value.setRoute(route);
        value.setBoardingStopName("Stop A");
        value.setDestinationStopName("Stop B");
        value.setFare(BigDecimal.TEN);
        value.setStatus(LocalFarePassStatus.VALID);
        value.setValidFrom(LocalDateTime.now().minusMinutes(1));
        value.setValidUntil(LocalDateTime.now().plusHours(1));
        value.setQrTokenHash(tokens.storedHash(value.getPassNumber()));
        return value;
    }
}
