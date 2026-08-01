package com.yatayat.backend.trip;

import com.yatayat.backend.config.SecurityConfig;
import com.yatayat.backend.controller.OperatorScheduledTripController;
import com.yatayat.backend.entity.*;
import com.yatayat.backend.repository.*;
import com.yatayat.backend.service.ScheduledTripService;
import com.yatayat.backend.service.TripOperationService;
import com.yatayat.backend.service.DriverNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = OperatorScheduledTripController.class)
@Import({SecurityConfig.class, ScheduledTripService.class})
class ScheduledTripIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private TransportOperatorRepository operatorRepository;
    @MockitoBean private RouteRepository routeRepository;
    @MockitoBean private BusRepository busRepository;
    @MockitoBean private DriverProfileRepository driverRepository;
    @MockitoBean private DriverOperatorAssociationRepository associationRepository;
    @MockitoBean private ScheduledTripRepository tripRepository;
    @MockitoBean private PassengerTripBookingRepository bookingRepository;
    @MockitoBean private TicketRepository ticketRepository;
    @MockitoBean private TripOperationService tripOperationService;
    @MockitoBean private DriverNotificationService driverNotificationService;

    private User operatorUser;
    private TransportOperator operator;
    private com.yatayat.backend.entity.Route route;
    private Bus bus;
    private DriverProfile driver;
    private DriverOperatorAssociation association;
    private LocalDateTime departure;
    private LocalDateTime arrival;

    @BeforeEach
    void setUp() {
        departure = LocalDateTime.now().plusDays(3).withNano(0);
        arrival = departure.plusHours(6);

        operatorUser = new User("Operator", "operator@example.com", "9800000001", "encoded", "OPERATOR");
        operatorUser.setId(1L);
        operator = new TransportOperator();
        operator.setId(2L);
        operator.setUser(operatorUser);
        operator.setName("Operator Company");
        operator.setVerificationStatus(OperatorVerificationStatus.APPROVED);

        route = new com.yatayat.backend.entity.Route();
        route.setId(3L);
        route.setCode("KTM-PKR-01");
        route.setName("Kathmandu to Pokhara");
        route.setOrigin("Kathmandu");
        route.setDestination("Pokhara");
        route.setDistanceKm(new BigDecimal("200.00"));
        route.setEstimatedDurationMinutes(360);
        route.setStatus(RouteStatus.ACTIVE);

        bus = new Bus();
        bus.setId(4L);
        bus.setBusNumber("BA-1-KHA-1234");
        bus.setBusName("Express Bus");
        bus.setBusType("DELUXE");
        bus.setSeatCapacity(40);
        bus.setStatus(BusStatus.APPROVED);
        bus.setOperator(operator);
        bus.setOperatorName(operator.getName());
        bus.setPermitExpiryDate(departure.toLocalDate().plusYears(1));
        bus.setInsuranceExpiryDate(departure.toLocalDate().plusYears(1));

        User driverUser = new User("Driver", "driver@example.com", "9800000002", "encoded", "DRIVER");
        driverUser.setId(5L);
        driver = new DriverProfile(driverUser);
        driver.setId(6L);
        driver.setVerificationStatus(DriverVerificationStatus.APPROVED);
        driver.setLicenseNumber("LIC-01");
        driver.setLicenseCategory("HEAVY");
        driver.setLicenseExpiryDate(departure.toLocalDate().plusYears(1));

        association = new DriverOperatorAssociation();
        association.setDriver(driver);
        association.setOperator(operator);
        association.setStatus(DriverOperatorAssociationStatus.ACTIVE);

        when(userRepository.findByEmailIgnoreCase("operator@example.com")).thenReturn(Optional.of(operatorUser));
        when(operatorRepository.findByUser(operatorUser)).thenReturn(Optional.of(operator));
    }

    @Test
    @WithMockUser(username = "operator@example.com", roles = "OPERATOR")
    void approvedOperatorLoadsFilteredEligibility() throws Exception {
        com.yatayat.backend.entity.Route inactive = route(30L, RouteStatus.INACTIVE);
        Bus invalidBus = bus(31L, BusStatus.PENDING);
        DriverProfile expiredDriver = driver(32L, DriverVerificationStatus.APPROVED, LocalDate.now().minusDays(1));
        DriverOperatorAssociation expiredAssociation = association(expiredDriver, DriverOperatorAssociationStatus.ACTIVE);

        when(routeRepository.findByStatusOrderByCodeAsc(RouteStatus.ACTIVE)).thenReturn(List.of(route));
        when(busRepository.findByOperatorOrderByCreatedAtDesc(operator)).thenReturn(List.of(bus, invalidBus));
        when(associationRepository.findByOperatorAndStatusOrderByInvitedAtDesc(operator, DriverOperatorAssociationStatus.ACTIVE))
                .thenReturn(List.of(association, expiredAssociation));

        mockMvc.perform(get("/api/operator/trips/eligibility"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.routes.length()").value(1))
                .andExpect(jsonPath("$.routes[0].status").doesNotExist())
                .andExpect(jsonPath("$.buses.length()").value(1))
                .andExpect(jsonPath("$.drivers.length()").value(1));
    }

    @Test
    @WithMockUser(username = "operator@example.com", roles = "OPERATOR")
    void localRoutesAreExcludedFromScheduledTripEligibility() throws Exception {
        com.yatayat.backend.entity.Route local = route(31L, RouteStatus.ACTIVE);
        local.setTripType(TripType.LOCAL);
        when(routeRepository.findByStatusOrderByCodeAsc(RouteStatus.ACTIVE)).thenReturn(List.of(route, local));
        when(busRepository.findByOperatorOrderByCreatedAtDesc(operator)).thenReturn(List.of(bus));
        when(associationRepository.findByOperatorAndStatusOrderByInvitedAtDesc(
                operator, DriverOperatorAssociationStatus.ACTIVE)).thenReturn(List.of(association));

        mockMvc.perform(get("/api/operator/trips/eligibility"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.routes.length()").value(1))
                .andExpect(jsonPath("$.routes[0].tripType").value("OUT_OF_VALLEY"));
    }

    @Test
    @WithMockUser(username = "operator@example.com", roles = "OPERATOR")
    void operatorCannotScheduleLocalRoute() throws Exception {
        route.setTripType(TripType.LOCAL);
        when(routeRepository.findById(3L)).thenReturn(Optional.of(route));

        mockMvc.perform(post("/api/operator/trips")
                        .contentType("application/json")
                        .content(request(departure, arrival, "850.00")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Only out-of-valley routes support scheduled seat booking"));
    }

    @Test
    @WithMockUser(username = "operator@example.com", roles = "OPERATOR")
    void approvedOperatorWithNoActiveRoutesReceivesEmptyRoutes() throws Exception {
        when(routeRepository.findByStatusOrderByCodeAsc(RouteStatus.ACTIVE)).thenReturn(List.of());
        when(busRepository.findByOperatorOrderByCreatedAtDesc(operator)).thenReturn(List.of(bus));
        when(associationRepository.findByOperatorAndStatusOrderByInvitedAtDesc(
                operator, DriverOperatorAssociationStatus.ACTIVE)).thenReturn(List.of(association));

        mockMvc.perform(get("/api/operator/trips/eligibility"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.routes.length()").value(0))
                .andExpect(jsonPath("$.buses.length()").value(1))
                .andExpect(jsonPath("$.drivers.length()").value(1));
    }

    @Test
    @WithMockUser(username = "operator@example.com", roles = "OPERATOR")
    void approvedOperatorWithNoEligibleBusesReceivesEmptyBuses() throws Exception {
        when(routeRepository.findByStatusOrderByCodeAsc(RouteStatus.ACTIVE)).thenReturn(List.of(route));
        when(busRepository.findByOperatorOrderByCreatedAtDesc(operator)).thenReturn(List.of());
        when(associationRepository.findByOperatorAndStatusOrderByInvitedAtDesc(
                operator, DriverOperatorAssociationStatus.ACTIVE)).thenReturn(List.of(association));

        mockMvc.perform(get("/api/operator/trips/eligibility"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.routes.length()").value(1))
                .andExpect(jsonPath("$.buses.length()").value(0))
                .andExpect(jsonPath("$.drivers.length()").value(1));
    }

    @Test
    @WithMockUser(username = "operator@example.com", roles = "OPERATOR")
    void approvedOperatorWithNoActiveDriverAssociationReceivesEmptyDrivers() throws Exception {
        when(routeRepository.findByStatusOrderByCodeAsc(RouteStatus.ACTIVE)).thenReturn(List.of(route));
        when(busRepository.findByOperatorOrderByCreatedAtDesc(operator)).thenReturn(List.of(bus));
        when(associationRepository.findByOperatorAndStatusOrderByInvitedAtDesc(
                operator, DriverOperatorAssociationStatus.ACTIVE)).thenReturn(List.of());

        mockMvc.perform(get("/api/operator/trips/eligibility"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.routes.length()").value(1))
                .andExpect(jsonPath("$.buses.length()").value(1))
                .andExpect(jsonPath("$.drivers.length()").value(0));
    }

    @Test
    @WithMockUser(username = "operator@example.com", roles = "OPERATOR")
    void pendingOperatorReceivesForbidden() throws Exception {
        operator.setVerificationStatus(OperatorVerificationStatus.PENDING);
        mockMvc.perform(get("/api/operator/trips/eligibility"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "operator@example.com", roles = "OPERATOR")
    void rejectedOperatorReceivesForbidden() throws Exception {
        operator.setVerificationStatus(OperatorVerificationStatus.REJECTED);
        mockMvc.perform(get("/api/operator/trips/eligibility"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedRequestReceivesUnauthorized() throws Exception {
        mockMvc.perform(get("/api/operator/trips"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "PASSENGER")
    void passengerCannotUseOperatorTrips() throws Exception {
        mockMvc.perform(get("/api/operator/trips")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "DRIVER")
    void driverCannotUseOperatorTrips() throws Exception {
        mockMvc.perform(get("/api/operator/trips")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCannotUseOperatorTrips() throws Exception {
        mockMvc.perform(get("/api/operator/trips")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "operator@example.com", roles = "OPERATOR")
    void validTripCreationReturnsCreated() throws Exception {
        prepareEligibleResources();
        when(tripRepository.saveAndFlush(any(ScheduledTrip.class))).thenAnswer(invocation -> {
            ScheduledTrip trip = invocation.getArgument(0);
            trip.setId(10L);
            return trip;
        });

        mockMvc.perform(post("/api/operator/trips")
                        .contentType("application/json").content(validRequest()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.seatCapacity").value(40))
                .andExpect(jsonPath("$.status").value("SCHEDULED"));
        verify(driverNotificationService).scheduledAssigned(any(ScheduledTrip.class));
    }

    @Test
    @WithMockUser(username = "operator@example.com", roles = "OPERATOR")
    void busFromAnotherOperatorReturnsNotFound() throws Exception {
        when(routeRepository.findById(3L)).thenReturn(Optional.of(route));
        when(busRepository.findLockedByIdAndOperator(4L, operator)).thenReturn(Optional.empty());
        mockMvc.perform(post("/api/operator/trips").contentType("application/json").content(validRequest()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "operator@example.com", roles = "OPERATOR")
    void driverWithoutActiveAssociationIsRejected() throws Exception {
        prepareRouteAndBus();
        when(driverRepository.findLockedById(6L)).thenReturn(Optional.of(driver));
        when(associationRepository.findByDriverAndOperator(driver, operator)).thenReturn(Optional.empty());
        mockMvc.perform(post("/api/operator/trips").contentType("application/json").content(validRequest()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "operator@example.com", roles = "OPERATOR")
    void inactiveRouteIsRejected() throws Exception {
        route.setStatus(RouteStatus.INACTIVE);
        when(routeRepository.findById(3L)).thenReturn(Optional.of(route));
        mockMvc.perform(post("/api/operator/trips").contentType("application/json").content(validRequest()))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username = "operator@example.com", roles = "OPERATOR")
    void unapprovedOrInvalidBusIsRejected() throws Exception {
        bus.setStatus(BusStatus.PENDING);
        prepareRouteAndBus();
        mockMvc.perform(post("/api/operator/trips").contentType("application/json").content(validRequest()))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username = "operator@example.com", roles = "OPERATOR")
    void unapprovedDriverIsRejected() throws Exception {
        driver.setVerificationStatus(DriverVerificationStatus.PENDING);
        prepareEligibleResources();
        mockMvc.perform(post("/api/operator/trips").contentType("application/json").content(validRequest()))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username = "operator@example.com", roles = "OPERATOR")
    void expiredDriverIsRejected() throws Exception {
        driver.setLicenseExpiryDate(departure.toLocalDate().minusDays(1));
        prepareEligibleResources();
        mockMvc.perform(post("/api/operator/trips").contentType("application/json").content(validRequest()))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username = "operator@example.com", roles = "OPERATOR")
    void invalidTimesAndFareAreRejected() throws Exception {
        String past = request(LocalDateTime.now().minusHours(2), LocalDateTime.now().minusHours(1), "500");
        mockMvc.perform(post("/api/operator/trips").contentType("application/json").content(past))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/operator/trips").contentType("application/json")
                        .content(request(departure, departure.minusMinutes(1), "500")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/operator/trips").contentType("application/json")
                        .content(request(departure, arrival, "0")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "operator@example.com", roles = "OPERATOR")
    void busOverlapReturnsConflict() throws Exception {
        prepareEligibleResources();
        when(tripRepository.findBusConflictsForUpdate(bus, departure, arrival, null))
                .thenReturn(List.of(trip(50L, TripStatus.SCHEDULED)));
        mockMvc.perform(post("/api/operator/trips").contentType("application/json").content(validRequest()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("BUS_SCHEDULE_CONFLICT")));
    }

    @Test
    @WithMockUser(username = "operator@example.com", roles = "OPERATOR")
    void driverOverlapReturnsConflict() throws Exception {
        prepareEligibleResources();
        when(tripRepository.findDriverConflictsForUpdate(driver, departure, arrival, null))
                .thenReturn(List.of(trip(51L, TripStatus.SCHEDULED)));
        mockMvc.perform(post("/api/operator/trips").contentType("application/json").content(validRequest()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("DRIVER_SCHEDULE_CONFLICT")));
    }

    @Test
    @WithMockUser(username = "operator@example.com", roles = "OPERATOR")
    void operatorListsOnlyOwnedTrips() throws Exception {
        when(tripRepository.findByOperatorOrderByDepartureAtDesc(operator))
                .thenReturn(List.of(trip(60L, TripStatus.SCHEDULED)));
        mockMvc.perform(get("/api/operator/trips"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(60));
    }

    @Test
    @WithMockUser(username = "operator@example.com", roles = "OPERATOR")
    void crossOperatorTripAccessReturnsNotFound() throws Exception {
        when(tripRepository.findByIdAndOperator(99L, operator)).thenReturn(Optional.empty());
        mockMvc.perform(get("/api/operator/trips/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "operator@example.com", roles = "OPERATOR")
    void onlyScheduledTripCanBeEdited() throws Exception {
        when(tripRepository.findByIdAndOperator(70L, operator))
                .thenReturn(Optional.of(trip(70L, TripStatus.BOARDING)));
        mockMvc.perform(put("/api/operator/trips/70").contentType("application/json").content(validRequest()))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username = "operator@example.com", roles = "OPERATOR")
    void confirmedBookingsBlockSensitiveEdits() throws Exception {
        ScheduledTrip scheduled = trip(71L, TripStatus.SCHEDULED);
        when(tripRepository.findByIdAndOperator(71L, operator)).thenReturn(Optional.of(scheduled));
        when(bookingRepository.existsByScheduledTripAndStatus(scheduled, BookingStatus.CONFIRMED))
                .thenReturn(true);

        mockMvc.perform(put("/api/operator/trips/71").contentType("application/json").content(validRequest()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Trips with confirmed bookings cannot be edited"));
    }

    @Test
    @WithMockUser(username = "operator@example.com", roles = "OPERATOR")
    void operatorCanReassignBeforeDepartureWithEligibleResources() throws Exception {
        ScheduledTrip scheduled = trip(72L, TripStatus.SCHEDULED);
        when(tripRepository.findByIdAndOperator(72L, operator)).thenReturn(Optional.of(scheduled));
        prepareEligibleResourcesForUpdate(72L);
        when(tripRepository.saveAndFlush(scheduled)).thenReturn(scheduled);

        mockMvc.perform(put("/api/operator/trips/72/assignment")
                        .contentType("application/json")
                        .content("{\"busId\":4,\"driverId\":6}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.busId").value(4))
                .andExpect(jsonPath("$.driverId").value(6))
                .andExpect(jsonPath("$.assignmentComplete").value(true));
    }

    @Test
    @WithMockUser(username = "operator@example.com", roles = "OPERATOR")
    void inProgressTripCannotBeReassigned() throws Exception {
        when(tripRepository.findByIdAndOperator(73L, operator))
                .thenReturn(Optional.of(trip(73L, TripStatus.IN_PROGRESS)));

        mockMvc.perform(put("/api/operator/trips/73/assignment")
                        .contentType("application/json")
                        .content("{\"busId\":4,\"driverId\":6}"))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username = "operator@example.com", roles = "OPERATOR")
    void cancellationTransitionRulesWork() throws Exception {
        ScheduledTrip scheduled = trip(80L, TripStatus.SCHEDULED);
        when(tripRepository.findByIdAndOperator(80L, operator)).thenReturn(Optional.of(scheduled));
        when(tripRepository.saveAndFlush(scheduled)).thenReturn(scheduled);
        mockMvc.perform(post("/api/operator/trips/80/cancel")
                        .contentType("application/json").content("{\"reason\":\"Service unavailable\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        ScheduledTrip completed = trip(81L, TripStatus.COMPLETED);
        when(tripRepository.findByIdAndOperator(81L, operator)).thenReturn(Optional.of(completed));
        mockMvc.perform(post("/api/operator/trips/81/cancel").contentType("application/json").content("{}"))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username = "operator@example.com", roles = "OPERATOR")
    void confirmedBookingsBlockOperatorCancellation() throws Exception {
        ScheduledTrip scheduled = trip(82L, TripStatus.SCHEDULED);
        when(tripRepository.findByIdAndOperator(82L, operator)).thenReturn(Optional.of(scheduled));
        when(bookingRepository.sumConfirmedSeatsByTrip(scheduled)).thenReturn(2L);

        mockMvc.perform(post("/api/operator/trips/82/cancel").contentType("application/json").content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Trips with confirmed bookings cannot be cancelled from this screen"));
    }

    @Test
    @WithMockUser(username = "operator@example.com", roles = "OPERATOR")
    void cancelledTripDoesNotBlockNewSchedule() throws Exception {
        prepareEligibleResources();
        when(tripRepository.findBusConflictsForUpdate(bus, departure, arrival, null)).thenReturn(List.of());
        when(tripRepository.findDriverConflictsForUpdate(driver, departure, arrival, null)).thenReturn(List.of());
        when(tripRepository.saveAndFlush(any(ScheduledTrip.class))).thenAnswer(invocation -> {
            ScheduledTrip value = invocation.getArgument(0); value.setId(90L); return value;
        });
        mockMvc.perform(post("/api/operator/trips").contentType("application/json").content(validRequest()))
                .andExpect(status().isCreated());
    }

    private void prepareRouteAndBus() {
        when(routeRepository.findById(3L)).thenReturn(Optional.of(route));
        when(busRepository.findLockedByIdAndOperator(4L, operator)).thenReturn(Optional.of(bus));
    }

    private void prepareEligibleResources() {
        prepareRouteAndBus();
        when(driverRepository.findLockedById(6L)).thenReturn(Optional.of(driver));
        when(associationRepository.findByDriverAndOperator(driver, operator)).thenReturn(Optional.of(association));
        when(tripRepository.findBusConflictsForUpdate(bus, departure, arrival, null)).thenReturn(List.of());
        when(tripRepository.findDriverConflictsForUpdate(driver, departure, arrival, null)).thenReturn(List.of());
    }

    private void prepareEligibleResourcesForUpdate(Long excludedId) {
        prepareRouteAndBus();
        when(driverRepository.findLockedById(6L)).thenReturn(Optional.of(driver));
        when(associationRepository.findByDriverAndOperator(driver, operator)).thenReturn(Optional.of(association));
        when(tripRepository.findBusConflictsForUpdate(bus, departure, arrival, excludedId)).thenReturn(List.of());
        when(tripRepository.findDriverConflictsForUpdate(driver, departure, arrival, excludedId)).thenReturn(List.of());
    }

    private String validRequest() { return request(departure, arrival, "850.00"); }
    private String request(LocalDateTime dep, LocalDateTime arr, String fare) {
        return """
                {"routeId":3,"busId":4,"driverId":6,"departureAt":"%s",
                 "estimatedArrivalAt":"%s","fare":%s,"boardingNotes":"Arrive early"}
                """.formatted(dep, arr, fare);
    }

    private ScheduledTrip trip(Long id, TripStatus status) {
        ScheduledTrip trip = new ScheduledTrip();
        trip.setId(id); trip.setOperator(operator); trip.setRoute(route); trip.setBus(bus); trip.setDriver(driver);
        trip.setDepartureAt(departure); trip.setEstimatedArrivalAt(arrival);
        trip.setFare(new BigDecimal("850.00")); trip.setSeatCapacitySnapshot(40); trip.setStatus(status);
        return trip;
    }

    private com.yatayat.backend.entity.Route route(Long id, RouteStatus status) {
        com.yatayat.backend.entity.Route value = new com.yatayat.backend.entity.Route();
        value.setId(id); value.setCode("R-" + id); value.setName("Route"); value.setOrigin("A"); value.setDestination("B");
        value.setDistanceKm(BigDecimal.ONE); value.setEstimatedDurationMinutes(10); value.setStatus(status); return value;
    }

    private Bus bus(Long id, BusStatus status) {
        Bus value = new Bus(); value.setId(id); value.setBusNumber("BUS-" + id); value.setBusName("Bus");
        value.setBusType("LOCAL"); value.setSeatCapacity(20); value.setStatus(status); value.setOperator(operator);
        value.setOperatorName(operator.getName()); return value;
    }

    private DriverProfile driver(Long id, DriverVerificationStatus status, LocalDate expiry) {
        User user = new User("Driver " + id, "driver" + id + "@example.com", "", "", "DRIVER");
        DriverProfile value = new DriverProfile(user); value.setId(id); value.setVerificationStatus(status);
        value.setLicenseNumber("LIC-" + id); value.setLicenseCategory("HEAVY"); value.setLicenseExpiryDate(expiry); return value;
    }

    private DriverOperatorAssociation association(DriverProfile value, DriverOperatorAssociationStatus status) {
        DriverOperatorAssociation item = new DriverOperatorAssociation(); item.setDriver(value); item.setOperator(operator); item.setStatus(status); return item;
    }
}
