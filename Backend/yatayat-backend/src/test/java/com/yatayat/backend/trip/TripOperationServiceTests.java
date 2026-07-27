package com.yatayat.backend.trip;

import com.yatayat.backend.dto.DriverTripOperationResponse;
import com.yatayat.backend.entity.*;
import com.yatayat.backend.repository.*;
import com.yatayat.backend.service.TripOperationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TripOperationServiceTests {
    @Mock private UserRepository userRepository;
    @Mock private DriverProfileRepository driverProfileRepository;
    @Mock private DriverOperatorAssociationRepository associationRepository;
    @Mock private TransportOperatorRepository operatorRepository;
    @Mock private ScheduledTripRepository tripRepository;
    @Mock private PassengerTripBookingRepository bookingRepository;
    @Mock private TicketRepository ticketRepository;

    private TripOperationService service;
    private User driverUser;
    private DriverProfile driver;
    private TransportOperator operator;
    private ScheduledTrip trip;

    @BeforeEach
    void setUp() {
        service = new TripOperationService(userRepository, driverProfileRepository,
                associationRepository, operatorRepository, tripRepository,
                bookingRepository, ticketRepository);
        driverUser = new User("Driver", "driver@example.com", "9800000000", "encoded", "DRIVER");
        driverUser.setId(1L);
        driver = new DriverProfile(driverUser);
        driver.setId(2L);
        driver.setVerificationStatus(DriverVerificationStatus.APPROVED);
        driver.setLicenseExpiryDate(LocalDateTime.now().plusDays(2).toLocalDate());

        User operatorUser = new User("Operator", "operator@example.com", "9800000001", "encoded", "OPERATOR");
        operator = new TransportOperator();
        operator.setId(3L);
        operator.setUser(operatorUser);
        operator.setName("Nepal Yatayat");
        operator.setVerificationStatus(OperatorVerificationStatus.APPROVED);

        com.yatayat.backend.entity.Route route = new com.yatayat.backend.entity.Route();
        route.setId(4L);
        route.setCode("KTM-PKR");
        route.setName("Kathmandu to Pokhara");
        route.setOrigin("Kathmandu");
        route.setDestination("Pokhara");

        Bus bus = new Bus();
        bus.setId(5L);
        bus.setBusName("Express");
        bus.setBusNumber("BA-1-KHA-1234");

        trip = new ScheduledTrip();
        trip.setId(50L);
        trip.setOperator(operator);
        trip.setRoute(route);
        trip.setBus(bus);
        trip.setDriver(driver);
        trip.setDepartureAt(LocalDateTime.now().plusHours(1));
        trip.setEstimatedArrivalAt(LocalDateTime.now().plusHours(7));
        trip.setFare(BigDecimal.valueOf(900));
        trip.setSeatCapacitySnapshot(40);
        trip.setStatus(TripStatus.SCHEDULED);
    }

    @Test
    void assignedDriverCanBoardStartAndFinishTrip() {
        mockApprovedDriver();
        mockAssociations();
        when(tripRepository.findByIdForOperation(50L)).thenReturn(Optional.of(trip));
        when(tripRepository.saveAndFlush(trip)).thenReturn(trip);
        when(bookingRepository.sumConfirmedSeatsByTrip(trip)).thenReturn(3L);
        when(ticketRepository.countByBookingScheduledTripAndStatus(trip, TicketStatus.USED)).thenReturn(1L);

        DriverTripOperationResponse boarding = service.beginBoarding("driver@example.com", 50L);
        assertThat(boarding.status()).isEqualTo("BOARDING");
        assertThat(boarding.startedAt()).isNull();

        DriverTripOperationResponse started = service.start("driver@example.com", 50L);
        assertThat(started.status()).isEqualTo("IN_PROGRESS");
        assertThat(started.startedAt()).isNotNull();
        assertThat(started.confirmedPassengers()).isEqualTo(3L);
        assertThat(started.boardedPassengers()).isEqualTo(1L);

        DriverTripOperationResponse finished = service.finish("driver@example.com", 50L);

        assertThat(finished.status()).isEqualTo("COMPLETED");
        assertThat(finished.endedAt()).isNotNull();
    }

    @Test
    void startTwiceAndFinishBeforeStartAreRejected() {
        mockApprovedDriver();
        mockAssociations();
        when(tripRepository.findByIdForOperation(50L)).thenReturn(Optional.of(trip));
        trip.setStatus(TripStatus.IN_PROGRESS);
        trip.setActualDepartureAt(LocalDateTime.now());

        assertThatThrownBy(() -> service.start("driver@example.com", 50L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.CONFLICT);

        trip.setStatus(TripStatus.SCHEDULED);
        trip.setActualDepartureAt(null);
        assertThatThrownBy(() -> service.finish("driver@example.com", 50L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void scheduledTripCannotSkipBoarding() {
        mockApprovedDriver();
        mockAssociations();
        when(tripRepository.findByIdForOperation(50L)).thenReturn(Optional.of(trip));

        assertThatThrownBy(() -> service.start("driver@example.com", 50L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void currentTripUsesRepositoryPriorityOrder() {
        mockApprovedDriver();
        when(associationRepository.findByDriverAndStatus(
                driver, DriverOperatorAssociationStatus.ACTIVE))
                .thenReturn(Optional.of(new DriverOperatorAssociation()));
        trip.setStatus(TripStatus.IN_PROGRESS);
        when(tripRepository.findDriverOperationalTrips(
                driver, List.of(TripStatus.SCHEDULED, TripStatus.BOARDING, TripStatus.IN_PROGRESS)))
                .thenReturn(List.of(trip));

        assertThat(service.currentDriverTrip("driver@example.com"))
                .hasValueSatisfying(current -> assertThat(current.status())
                        .isEqualTo("IN_PROGRESS"));
    }

    @Test
    void anotherDriverCannotOperateAssignedTrip() {
        mockApprovedDriver();
        when(associationRepository.findByDriverAndStatus(driver, DriverOperatorAssociationStatus.ACTIVE))
                .thenReturn(Optional.of(new DriverOperatorAssociation()));
        DriverProfile other = new DriverProfile(new User("Other", "other@example.com", "", "", "DRIVER"));
        other.setId(99L);
        trip.setDriver(other);
        when(tripRepository.findByIdForOperation(50L)).thenReturn(Optional.of(trip));

        assertThatThrownBy(() -> service.start("driver@example.com", 50L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void operatorLiveTripsReturnsCounts() {
        User operatorUser = operator.getUser();
        when(userRepository.findByEmailIgnoreCase("operator@example.com")).thenReturn(Optional.of(operatorUser));
        when(operatorRepository.findByUser(operatorUser)).thenReturn(Optional.of(operator));
        when(tripRepository.findOperatorLiveTrips(operator,
                List.of(TripStatus.BOARDING, TripStatus.IN_PROGRESS, TripStatus.COMPLETED)))
                .thenReturn(List.of(trip));
        when(bookingRepository.sumConfirmedSeatsByTrip(trip)).thenReturn(5L);
        when(ticketRepository.countByBookingScheduledTripAndStatus(trip, TicketStatus.USED)).thenReturn(2L);

        var live = service.operatorLiveTrips("operator@example.com");

        assertThat(live).hasSize(1);
        assertThat(live.get(0).passengerCount()).isEqualTo(5L);
        assertThat(live.get(0).boardedCount()).isEqualTo(2L);
    }

    private void mockApprovedDriver() {
        when(userRepository.findByEmailIgnoreCase("driver@example.com")).thenReturn(Optional.of(driverUser));
        when(driverProfileRepository.findByUser(driverUser)).thenReturn(Optional.of(driver));
    }

    private void mockAssociations() {
        DriverOperatorAssociation association = new DriverOperatorAssociation();
        association.setDriver(driver);
        association.setOperator(operator);
        association.setStatus(DriverOperatorAssociationStatus.ACTIVE);
        when(associationRepository.findByDriverAndStatus(driver, DriverOperatorAssociationStatus.ACTIVE))
                .thenReturn(Optional.of(association));
        when(associationRepository.findByDriverAndOperator(driver, operator))
                .thenReturn(Optional.of(association));
    }
}
