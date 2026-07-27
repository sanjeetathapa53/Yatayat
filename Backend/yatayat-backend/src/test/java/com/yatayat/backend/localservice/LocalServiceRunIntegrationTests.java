package com.yatayat.backend.localservice;

import com.yatayat.backend.config.SecurityConfig;
import com.yatayat.backend.controller.DriverLocalServiceController;
import com.yatayat.backend.controller.OperatorLocalServiceController;
import com.yatayat.backend.entity.*;
import com.yatayat.backend.repository.*;
import com.yatayat.backend.service.LocalServiceRunService;
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
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {OperatorLocalServiceController.class, DriverLocalServiceController.class})
@Import({SecurityConfig.class, LocalServiceRunService.class})
class LocalServiceRunIntegrationTests {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private TransportOperatorRepository operatorRepository;
    @MockitoBean private RouteRepository routeRepository;
    @MockitoBean private RouteStopRepository routeStopRepository;
    @MockitoBean private BusRepository busRepository;
    @MockitoBean private DriverProfileRepository driverRepository;
    @MockitoBean private DriverOperatorAssociationRepository associationRepository;
    @MockitoBean private ScheduledTripRepository scheduledTripRepository;
    @MockitoBean private LocalServiceRunRepository localRunRepository;

    private User operatorUser;
    private TransportOperator operator;
    private com.yatayat.backend.entity.Route localRoute;
    private com.yatayat.backend.entity.Route outOfValleyRoute;
    private Bus bus;
    private DriverProfile driver;
    private DriverOperatorAssociation association;
    private LocalDate serviceDate;

    @BeforeEach
    void setUp() {
        serviceDate = LocalDate.now().plusDays(2);

        operatorUser = new User("Operator", "operator@example.com", "9800000001", "encoded", "OPERATOR");
        operatorUser.setId(1L);
        operator = new TransportOperator();
        operator.setId(2L);
        operator.setUser(operatorUser);
        operator.setName("Local Operator");
        operator.setVerificationStatus(OperatorVerificationStatus.APPROVED);

        localRoute = route(3L, "LOCAL-R1", TripType.LOCAL, RouteStatus.ACTIVE);
        outOfValleyRoute = route(30L, "OOV-R1", TripType.OUT_OF_VALLEY, RouteStatus.ACTIVE);

        bus = new Bus();
        bus.setId(4L);
        bus.setBusNumber("BA-1-KHA-1234");
        bus.setBusName("City Runner");
        bus.setBusType("CITY");
        bus.setSeatCapacity(32);
        bus.setStatus(BusStatus.APPROVED);
        bus.setOperator(operator);
        bus.setOperatorName(operator.getName());
        bus.setPermitExpiryDate(serviceDate.plusYears(1));
        bus.setInsuranceExpiryDate(serviceDate.plusYears(1));

        User driverUser = new User("Driver", "driver@example.com", "9800000002", "encoded", "DRIVER");
        driverUser.setId(5L);
        driver = new DriverProfile(driverUser);
        driver.setId(6L);
        driver.setVerificationStatus(DriverVerificationStatus.APPROVED);
        driver.setLicenseNumber("LIC-01");
        driver.setLicenseCategory("HEAVY");
        driver.setLicenseExpiryDate(serviceDate.plusYears(1));

        association = new DriverOperatorAssociation();
        association.setDriver(driver);
        association.setOperator(operator);
        association.setStatus(DriverOperatorAssociationStatus.ACTIVE);

        when(userRepository.findByEmailIgnoreCase("operator@example.com")).thenReturn(Optional.of(operatorUser));
        when(operatorRepository.findByUser(operatorUser)).thenReturn(Optional.of(operator));
        when(routeStopRepository.findByRouteIdAndActiveTrueOrderByStopOrderAsc(3L)).thenReturn(stops(localRoute));
        when(routeStopRepository.findByRouteIdAndActiveTrueOrderByStopOrderAsc(30L)).thenReturn(stops(outOfValleyRoute));
    }

    @Test
    @WithMockUser(username = "operator@example.com", roles = "OPERATOR")
    void approvedOperatorCreatesValidLocalRun() throws Exception {
        prepareEligibleResources();
        when(localRunRepository.saveAndFlush(any(LocalServiceRun.class))).thenAnswer(invocation -> {
            LocalServiceRun run = invocation.getArgument(0);
            run.setId(20L);
            return run;
        });

        mockMvc.perform(post("/api/operator/local-services")
                        .contentType("application/json")
                        .content(validRequest()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(20))
                .andExpect(jsonPath("$.routeCode").value("LOCAL-R1"))
                .andExpect(jsonPath("$.status").value("PLANNED"))
                .andExpect(jsonPath("$.orderedStops.length()").value(2));
    }

    @Test
    void unauthenticatedOperatorRequestReceivesUnauthorized() throws Exception {
        mockMvc.perform(get("/api/operator/local-services"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "PASSENGER")
    void passengerCannotUseOperatorLocalServices() throws Exception {
        mockMvc.perform(get("/api/operator/local-services")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "DRIVER")
    void driverCannotUseOperatorLocalWrites() throws Exception {
        mockMvc.perform(post("/api/operator/local-services")
                        .contentType("application/json")
                        .content(validRequest()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCannotUseOperatorLocalServices() throws Exception {
        mockMvc.perform(get("/api/operator/local-services")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "operator@example.com", roles = "OPERATOR")
    void pendingOperatorReceivesForbidden() throws Exception {
        operator.setVerificationStatus(OperatorVerificationStatus.PENDING);
        mockMvc.perform(get("/api/operator/local-services/options"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "operator@example.com", roles = "OPERATOR")
    void optionsReturnOnlyLocalRoutesAndEligibleResources() throws Exception {
        when(routeRepository.findByStatusAndTripTypeOrderByCodeAsc(RouteStatus.ACTIVE, TripType.LOCAL))
                .thenReturn(List.of(localRoute));
        when(busRepository.findByOperatorOrderByCreatedAtDesc(operator)).thenReturn(List.of(bus));
        when(associationRepository.findByOperatorAndStatusOrderByInvitedAtDesc(
                operator, DriverOperatorAssociationStatus.ACTIVE)).thenReturn(List.of(association));

        mockMvc.perform(get("/api/operator/local-services/options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.routes.length()").value(1))
                .andExpect(jsonPath("$.routes[0].tripType").value("LOCAL"))
                .andExpect(jsonPath("$.buses.length()").value(1))
                .andExpect(jsonPath("$.drivers.length()").value(1));
    }

    @Test
    @WithMockUser(username = "operator@example.com", roles = "OPERATOR")
    void outOfValleyRouteRejected() throws Exception {
        when(routeRepository.findById(30L)).thenReturn(Optional.of(outOfValleyRoute));

        mockMvc.perform(post("/api/operator/local-services")
                        .contentType("application/json")
                        .content(request(30L)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Only local routes can be assigned to local services."));
    }

    @Test
    @WithMockUser(username = "operator@example.com", roles = "OPERATOR")
    void inactiveRouteRejected() throws Exception {
        localRoute.setStatus(RouteStatus.INACTIVE);
        when(routeRepository.findById(3L)).thenReturn(Optional.of(localRoute));

        mockMvc.perform(post("/api/operator/local-services")
                        .contentType("application/json")
                        .content(validRequest()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Selected local route is not active."));
    }

    @Test
    @WithMockUser(username = "operator@example.com", roles = "OPERATOR")
    void overlappingLocalBusAssignmentRejected() throws Exception {
        prepareEligibleResources();
        when(localRunRepository.findBusConflictsForUpdate(eq(bus), eq(serviceDate), any(), any(), anyList(), isNull()))
                .thenReturn(List.of(existingRun(LocalServiceRunStatus.PLANNED)));

        mockMvc.perform(post("/api/operator/local-services")
                        .contentType("application/json")
                        .content(validRequest()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Bus is already assigned during this time."));
    }

    @Test
    @WithMockUser(username = "operator@example.com", roles = "OPERATOR")
    void overlappingOutOfValleyDriverAssignmentRejected() throws Exception {
        prepareEligibleResources();
        when(scheduledTripRepository.findDriverConflictsForUpdate(eq(driver), any(), any(), isNull()))
                .thenReturn(List.of(new ScheduledTrip()));

        mockMvc.perform(post("/api/operator/local-services")
                        .contentType("application/json")
                        .content(validRequest()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Driver is already assigned during this time."));
    }

    @Test
    @WithMockUser(username = "operator@example.com", roles = "OPERATOR")
    void cancelledRunCanBeReusedWithoutConflict() throws Exception {
        prepareEligibleResources();
        when(localRunRepository.findBusConflictsForUpdate(eq(bus), eq(serviceDate), any(), any(), anyList(), isNull()))
                .thenReturn(List.of());
        when(localRunRepository.findDriverConflictsForUpdate(eq(driver), eq(serviceDate), any(), any(), anyList(), isNull()))
                .thenReturn(List.of());
        when(localRunRepository.saveAndFlush(any(LocalServiceRun.class))).thenAnswer(invocation -> {
            LocalServiceRun run = invocation.getArgument(0);
            run.setId(40L);
            return run;
        });

        mockMvc.perform(post("/api/operator/local-services")
                        .contentType("application/json")
                        .content(validRequest()))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "operator@example.com", roles = "OPERATOR")
    void crossOperatorRunIsHidden() throws Exception {
        when(localRunRepository.findByIdAndOperator(99L, operator)).thenReturn(Optional.empty());
        mockMvc.perform(get("/api/operator/local-services/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "operator@example.com", roles = "OPERATOR")
    void plannedRunCanBeCancelled() throws Exception {
        LocalServiceRun run = existingRun(LocalServiceRunStatus.PLANNED);
        when(localRunRepository.findByIdAndOperator(20L, operator)).thenReturn(Optional.of(run));
        when(localRunRepository.saveAndFlush(run)).thenReturn(run);

        mockMvc.perform(patch("/api/operator/local-services/20/cancel")
                        .contentType("application/json")
                        .content("{\"reason\":\"demo\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @WithMockUser(username = "driver@example.com", roles = "DRIVER")
    void assignedDriverSeesOwnLocalRun() throws Exception {
        when(userRepository.findByEmailIgnoreCase("driver@example.com")).thenReturn(Optional.of(driver.getUser()));
        when(driverRepository.findByUser(driver.getUser())).thenReturn(Optional.of(driver));
        when(localRunRepository.findByDriverOrderByServiceDateAscPlannedStartTimeAsc(driver))
                .thenReturn(List.of(existingRun(LocalServiceRunStatus.PLANNED)));

        mockMvc.perform(get("/api/driver/local-services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].driverName").value("Driver"));
    }

    @Test
    @WithMockUser(username = "driver@example.com", roles = "DRIVER")
    void currentLocalServicePrioritizesRepositoryOperationalResult() throws Exception {
        mockOperationalDriver();
        LocalServiceRun active = existingRun(LocalServiceRunStatus.IN_SERVICE);
        active.setServiceDate(LocalDate.now());
        active.setActualStartedAt(LocalDateTime.now().minusMinutes(10));
        when(localRunRepository.findDriverOperationalRuns(driver, LocalDate.now()))
                .thenReturn(List.of(active));

        mockMvc.perform(get("/api/driver/local-services/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(20))
                .andExpect(jsonPath("$.status").value("IN_SERVICE"));
    }

    @Test
    @WithMockUser(username = "driver@example.com", roles = "DRIVER")
    void assignedDriverCanStartAndFinishLocalService() throws Exception {
        mockOperationalDriver();
        LocalServiceRun run = existingRun(LocalServiceRunStatus.PLANNED);
        run.setServiceDate(LocalDate.now());
        when(localRunRepository.findByIdAndDriverForOperation(20L, driver))
                .thenReturn(Optional.of(run));
        when(associationRepository.findByDriverAndOperator(driver, operator))
                .thenReturn(Optional.of(association));
        when(localRunRepository.saveAndFlush(run)).thenReturn(run);

        mockMvc.perform(post("/api/driver/local-services/20/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_SERVICE"))
                .andExpect(jsonPath("$.actualStartedAt").isNotEmpty());

        mockMvc.perform(post("/api/driver/local-services/20/finish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.actualCompletedAt").isNotEmpty());
    }

    @Test
    @WithMockUser(username = "driver@example.com", roles = "DRIVER")
    void driverCannotOperateAnotherDriversLocalService() throws Exception {
        mockOperationalDriver();
        when(localRunRepository.findByIdAndDriverForOperation(88L, driver))
                .thenReturn(Optional.empty());

        mockMvc.perform(post("/api/driver/local-services/88/start"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "driver@example.com", roles = "DRIVER")
    void inactiveOperatorAssociationPreventsLocalServiceStart() throws Exception {
        mockOperationalDriver();
        LocalServiceRun run = existingRun(LocalServiceRunStatus.PLANNED);
        run.setServiceDate(LocalDate.now());
        association.setStatus(DriverOperatorAssociationStatus.REMOVED);
        when(localRunRepository.findByIdAndDriverForOperation(20L, driver))
                .thenReturn(Optional.of(run));
        when(associationRepository.findByDriverAndOperator(driver, operator))
                .thenReturn(Optional.of(association));

        mockMvc.perform(post("/api/driver/local-services/20/start"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message")
                        .value("Active operator association is required for this local service."));
    }

    @Test
    @WithMockUser(username = "driver@example.com", roles = "DRIVER")
    void expiredDriverLicencePreventsLocalServiceStart() throws Exception {
        driver.setLicenseExpiryDate(LocalDate.now().minusDays(1));
        when(userRepository.findByEmailIgnoreCase("driver@example.com"))
                .thenReturn(Optional.of(driver.getUser()));
        when(driverRepository.findByUser(driver.getUser())).thenReturn(Optional.of(driver));

        mockMvc.perform(post("/api/driver/local-services/20/start"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "driver@example.com", roles = "DRIVER")
    void unrelatedDriverCannotReadRun() throws Exception {
        when(userRepository.findByEmailIgnoreCase("driver@example.com")).thenReturn(Optional.of(driver.getUser()));
        when(driverRepository.findByUser(driver.getUser())).thenReturn(Optional.of(driver));
        when(localRunRepository.findByIdAndDriver(88L, driver)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/driver/local-services/88"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "operator@example.com", roles = "OPERATOR")
    void endBeforeStartRejected() throws Exception {
        mockMvc.perform(post("/api/operator/local-services")
                        .contentType("application/json")
                        .content("""
                                {"routeId":3,"busId":4,"driverId":6,"serviceDate":"%s","plannedStartTime":"11:00","plannedEndTime":"10:00"}
                                """.formatted(serviceDate)))
                .andExpect(status().isBadRequest());
    }

    private void prepareEligibleResources() {
        when(routeRepository.findById(3L)).thenReturn(Optional.of(localRoute));
        when(busRepository.findLockedByIdAndOperator(4L, operator)).thenReturn(Optional.of(bus));
        when(driverRepository.findLockedById(6L)).thenReturn(Optional.of(driver));
        when(associationRepository.findByDriverAndOperator(driver, operator)).thenReturn(Optional.of(association));
        when(localRunRepository.findBusConflictsForUpdate(any(), any(), any(), any(), anyList(), any()))
                .thenReturn(List.of());
        when(localRunRepository.findDriverConflictsForUpdate(any(), any(), any(), any(), anyList(), any()))
                .thenReturn(List.of());
        when(scheduledTripRepository.findBusConflictsForUpdate(any(), any(), any(), any()))
                .thenReturn(List.of());
        when(scheduledTripRepository.findDriverConflictsForUpdate(any(), any(), any(), any()))
                .thenReturn(List.of());
    }

    private void mockOperationalDriver() {
        when(userRepository.findByEmailIgnoreCase("driver@example.com"))
                .thenReturn(Optional.of(driver.getUser()));
        when(driverRepository.findByUser(driver.getUser())).thenReturn(Optional.of(driver));
    }

    private LocalServiceRun existingRun(LocalServiceRunStatus status) {
        LocalServiceRun run = new LocalServiceRun();
        run.setId(20L);
        run.setOperator(operator);
        run.setRoute(localRoute);
        run.setBus(bus);
        run.setDriver(driver);
        run.setServiceDate(serviceDate);
        run.setPlannedStartTime(LocalTime.of(9, 0));
        run.setPlannedEndTime(LocalTime.of(11, 0));
        run.setStatus(status);
        run.setCreatedAt(LocalDateTime.now());
        run.setUpdatedAt(LocalDateTime.now());
        return run;
    }

    private com.yatayat.backend.entity.Route route(Long id, String code, TripType type, RouteStatus status) {
        com.yatayat.backend.entity.Route route = new com.yatayat.backend.entity.Route();
        route.setId(id);
        route.setCode(code);
        route.setName(code + " Route");
        route.setOrigin("Gongabu Bus Park");
        route.setDestination("Ratnapark");
        route.setDistanceKm(new BigDecimal("8.00"));
        route.setEstimatedDurationMinutes(35);
        route.setTripType(type);
        route.setStatus(status);
        return route;
    }

    private List<RouteStop> stops(com.yatayat.backend.entity.Route route) {
        return List.of(routeStop(route, 1L, "Gongabu Bus Park", 1), routeStop(route, 2L, "Ratnapark", 2));
    }

    private RouteStop routeStop(com.yatayat.backend.entity.Route route, Long stopId, String name, int order) {
        BusStop stop = new BusStop();
        stop.setId(stopId);
        stop.setName(name);
        stop.setLandmark(name + " landmark");
        RouteStop routeStop = new RouteStop();
        routeStop.setId(100L + stopId);
        routeStop.setRoute(route);
        routeStop.setBusStop(stop);
        routeStop.setStopOrder(order);
        routeStop.setEstimatedMinutesFromStart((order - 1) * 35);
        routeStop.setCumulativeFare(new BigDecimal(order == 1 ? "0" : "45"));
        routeStop.setActive(true);
        return routeStop;
    }

    private String validRequest() {
        return request(3L);
    }

    private String request(Long routeId) {
        return """
                {"routeId":%d,"busId":4,"driverId":6,"serviceDate":"%s","plannedStartTime":"09:00","plannedEndTime":"11:00","notes":"Morning local service"}
                """.formatted(routeId, serviceDate);
    }
}
