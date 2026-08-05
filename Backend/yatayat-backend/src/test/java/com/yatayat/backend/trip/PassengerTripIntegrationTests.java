package com.yatayat.backend.trip;

import com.yatayat.backend.config.SecurityConfig;
import com.yatayat.backend.controller.PassengerTripController;
import com.yatayat.backend.entity.*;
import com.yatayat.backend.repository.ScheduledTripRepository;
import com.yatayat.backend.repository.RouteRepository;
import com.yatayat.backend.repository.UserRepository;
import com.yatayat.backend.service.PassengerTripService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PassengerTripController.class)
@Import({SecurityConfig.class, PassengerTripService.class})
class PassengerTripIntegrationTests {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private ScheduledTripRepository tripRepository;
    @MockitoBean private RouteRepository routeRepository;

    private User passenger;
    private ScheduledTrip trip;

    @BeforeEach
    void setUp() {
        passenger = new User("Passenger", "passenger@example.com", "", "encoded", "PASSENGER");
        when(userRepository.findByEmailIgnoreCase("passenger@example.com"))
                .thenReturn(Optional.of(passenger));
        trip = visibleTrip(10L, LocalDateTime.now().plusDays(2));
    }

    @Test
    @WithMockUser(username = "passenger@example.com", roles = "PASSENGER")
    void validSearchReturnsPassengerSafeTrip() throws Exception {
        when(tripRepository.searchPassengerVisible(eq("Kathmandu"), eq("Pokhara"),
                any(), anyList(), isNull(), isNull())).thenReturn(List.of(trip));

        mockMvc.perform(get("/api/passenger/trips/search")
                        .param("origin", "Kathmandu").param("destination", "Pokhara"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tripId").value(10))
                .andExpect(jsonPath("$[0].operatorName").value("Safe Travels"))
                .andExpect(jsonPath("$[0].busNumber").value("BA-1-KHA-1000"))
                .andExpect(jsonPath("$[0].driverName").doesNotExist())
                .andExpect(jsonPath("$[0].email").doesNotExist())
                .andExpect(jsonPath("$[0].licenseNumber").doesNotExist())
                .andExpect(jsonPath("$[0].version").doesNotExist());
    }

    @Test
    @WithMockUser(username = "passenger@example.com", roles = "PASSENGER")
    void routeOptionsReturnSafeActiveOutOfValleyLocations() throws Exception {
        com.yatayat.backend.entity.Route route = new com.yatayat.backend.entity.Route();
        route.setId(101L);
        route.setCode("KTM-PKR");
        route.setName("Kathmandu to Pokhara");
        route.setOrigin("Kathmandu");
        route.setDestination("Pokhara");
        route.setStatus(RouteStatus.ACTIVE);
        route.setTripType(TripType.OUT_OF_VALLEY);
        when(routeRepository.findByStatusAndTripTypeOrderByCodeAsc(
                RouteStatus.ACTIVE, TripType.OUT_OF_VALLEY)).thenReturn(List.of(route));

        mockMvc.perform(get("/api/passenger/trips/route-options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].routeId").value(101))
                .andExpect(jsonPath("$[0].origin").value("Kathmandu"))
                .andExpect(jsonPath("$[0].destination").value("Pokhara"))
                .andExpect(jsonPath("$[0].routeName").value("Kathmandu to Pokhara"))
                .andExpect(jsonPath("$[0].operatorName").doesNotExist());
    }

    @Test
    void anonymousRouteOptionsRequestIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/passenger/trips/route-options"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "passenger@example.com", roles = "PASSENGER")
    void futureSearchableLocalTripAppearsWithLocalType() throws Exception {
        trip.getRoute().setTripType(TripType.LOCAL);
        when(tripRepository.searchPassengerVisible(eq("Gongabu"), eq("Ratnapark"),
                any(), anyList(), isNull(), isNull())).thenReturn(List.of(trip));

        mockMvc.perform(get("/api/passenger/trips/search")
                        .param("origin", "Gongabu").param("destination", "Ratnapark"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].tripId").value(10))
                .andExpect(jsonPath("$[0].tripType").value("LOCAL"));
    }

    @Test
    @WithMockUser(username = "passenger@example.com", roles = "PASSENGER")
    void futureSearchableOutsideValleyTripStillAppears() throws Exception {
        trip.getRoute().setTripType(TripType.OUT_OF_VALLEY);
        when(tripRepository.searchPassengerVisible(eq("Kathmandu"), eq("Pokhara"),
                any(), anyList(), isNull(), isNull())).thenReturn(List.of(trip));

        mockMvc.perform(get("/api/passenger/trips/search")
                        .param("origin", "Kathmandu").param("destination", "Pokhara"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].tripType").value("OUT_OF_VALLEY"));
    }

    @Test
    @WithMockUser(username = "passenger@example.com", roles = "PASSENGER")
    void searchTrimsWhitespaceAndPreservesCaseInsensitiveRepositoryContract() throws Exception {
        when(tripRepository.searchPassengerVisible(eq("kAtHmAnDu"), eq("POKHARA"),
                any(), anyList(), isNull(), isNull())).thenReturn(List.of());
        mockMvc.perform(get("/api/passenger/trips/search")
                        .param("origin", "  kAtHmAnDu ").param("destination", " POKHARA  "))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        verify(tripRepository).searchPassengerVisible(eq("kAtHmAnDu"), eq("POKHARA"),
                any(), anyList(), isNull(), isNull());
    }

    @Test
    @WithMockUser(username = "passenger@example.com", roles = "PASSENGER")
    void reverseDirectionIsSearchedAsASeparateDirection() throws Exception {
        when(tripRepository.searchPassengerVisible(eq("Pokhara"), eq("Kathmandu"),
                any(), anyList(), isNull(), isNull())).thenReturn(List.of());
        mockMvc.perform(get("/api/passenger/trips/search")
                        .param("origin", "Pokhara").param("destination", "Kathmandu"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        verify(tripRepository).searchPassengerVisible(eq("Pokhara"), eq("Kathmandu"),
                any(), anyList(), isNull(), isNull());
    }

    @Test
    @WithMockUser(username = "passenger@example.com", roles = "PASSENGER")
    void repositoryDepartureOrderingIsPreservedInResponse() throws Exception {
        ScheduledTrip earlier = visibleTrip(30L, LocalDateTime.now().plusDays(1));
        ScheduledTrip later = visibleTrip(31L, LocalDateTime.now().plusDays(2));
        when(tripRepository.searchPassengerVisible(anyString(), anyString(), any(), anyList(),
                isNull(), isNull())).thenReturn(List.of(earlier, later));
        mockMvc.perform(get("/api/passenger/trips/search").param("origin", "Kathmandu")
                        .param("destination", "Pokhara"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tripId").value(30))
                .andExpect(jsonPath("$[1].tripId").value(31));
    }

    @Test
    @WithMockUser(username = "passenger@example.com", roles = "PASSENGER")
    void dateFilterUsesOneDayWindow() throws Exception {
        LocalDate date = LocalDate.now().plusDays(2);
        when(tripRepository.searchPassengerVisible(anyString(), anyString(), any(), anyList(),
                eq(date.atStartOfDay()), eq(date.plusDays(1).atStartOfDay()))).thenReturn(List.of(trip));
        mockMvc.perform(get("/api/passenger/trips/search").param("origin", "Kathmandu")
                        .param("destination", "Pokhara").param("date", date.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @WithMockUser(username = "passenger@example.com", roles = "PASSENGER")
    void emptyResultReturnsOkWithEmptyArray() throws Exception {
        when(tripRepository.searchPassengerVisible(anyString(), anyString(), any(), anyList(),
                isNull(), isNull())).thenReturn(List.of());
        mockMvc.perform(get("/api/passenger/trips/search").param("origin", "A")
                        .param("destination", "B"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @WithMockUser(username = "passenger@example.com", roles = "PASSENGER")
    void blankSearchIsRejected() throws Exception {
        mockMvc.perform(get("/api/passenger/trips/search").param("origin", "  ")
                        .param("destination", "Pokhara"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Origin is required"));
    }

    @Test
    @WithMockUser(username = "passenger@example.com", roles = "PASSENGER")
    void repositoryResultsWithInvisibleStatusOrPastDepartureAreExcluded() throws Exception {
        ScheduledTrip cancelled = visibleTrip(11L, LocalDateTime.now().plusDays(2));
        cancelled.setStatus(TripStatus.CANCELLED);
        ScheduledTrip completed = visibleTrip(12L, LocalDateTime.now().plusDays(2));
        completed.setStatus(TripStatus.COMPLETED);
        ScheduledTrip inProgress = visibleTrip(13L, LocalDateTime.now().plusDays(2));
        inProgress.setStatus(TripStatus.IN_PROGRESS);
        ScheduledTrip past = visibleTrip(14L, LocalDateTime.now().minusDays(1));
        when(tripRepository.searchPassengerVisible(anyString(), anyString(), any(), anyList(),
                isNull(), isNull())).thenReturn(List.of(cancelled, completed, inProgress, past));
        mockMvc.perform(get("/api/passenger/trips/search").param("origin", "Kathmandu")
                        .param("destination", "Pokhara"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @WithMockUser(username = "passenger@example.com", roles = "PASSENGER")
    void invalidRouteOperatorBusAndDriverAreExcluded() throws Exception {
        ScheduledTrip inactiveRoute = visibleTrip(20L, LocalDateTime.now().plusDays(2));
        inactiveRoute.getRoute().setStatus(RouteStatus.INACTIVE);
        ScheduledTrip pendingOperator = visibleTrip(21L, LocalDateTime.now().plusDays(2));
        pendingOperator.getOperator().setVerificationStatus(OperatorVerificationStatus.PENDING);
        ScheduledTrip rejectedBus = visibleTrip(22L, LocalDateTime.now().plusDays(2));
        rejectedBus.getBus().setStatus(BusStatus.REJECTED);
        ScheduledTrip pendingDriver = visibleTrip(23L, LocalDateTime.now().plusDays(2));
        pendingDriver.getDriver().setVerificationStatus(DriverVerificationStatus.PENDING);
        when(tripRepository.searchPassengerVisible(anyString(), anyString(), any(), anyList(),
                isNull(), isNull())).thenReturn(List.of(inactiveRoute, pendingOperator, rejectedBus, pendingDriver));
        mockMvc.perform(get("/api/passenger/trips/search").param("origin", "Kathmandu")
                        .param("destination", "Pokhara"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @WithMockUser(username = "passenger@example.com", roles = "PASSENGER")
    void detailsReturnSafeDataAndBoardingNotes() throws Exception {
        when(tripRepository.findPassengerVisibleById(eq(10L), any(), anyList()))
                .thenReturn(Optional.of(trip));
        mockMvc.perform(get("/api/passenger/trips/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.boardingNotes").value("Board at gate 2"))
                .andExpect(jsonPath("$.driverName").doesNotExist())
                .andExpect(jsonPath("$.operatorEmail").doesNotExist());
    }

    @Test
    @WithMockUser(username = "passenger@example.com", roles = "PASSENGER")
    void unknownOrInvisibleTripReturnsNotFound() throws Exception {
        when(tripRepository.findPassengerVisibleById(eq(99L), any(), anyList()))
                .thenReturn(Optional.empty());
        mockMvc.perform(get("/api/passenger/trips/99")).andExpect(status().isNotFound());
    }

    @Test
    void anonymousRequestIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/passenger/trips/search").param("origin", "A")
                .param("destination", "B")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "operator@example.com", roles = "OPERATOR")
    void operatorIsForbidden() throws Exception {
        User operator = new User("Operator", "operator@example.com", "", "", "OPERATOR");
        when(userRepository.findByEmailIgnoreCase("operator@example.com")).thenReturn(Optional.of(operator));
        mockMvc.perform(get("/api/passenger/trips/search").param("origin", "A")
                .param("destination", "B")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "driver@example.com", roles = "DRIVER")
    void driverIsForbidden() throws Exception {
        User driver = new User("Driver", "driver@example.com", "", "", "DRIVER");
        when(userRepository.findByEmailIgnoreCase("driver@example.com")).thenReturn(Optional.of(driver));
        mockMvc.perform(get("/api/passenger/trips/search").param("origin", "A")
                .param("destination", "B")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void adminIsForbidden() throws Exception {
        User admin = new User("Admin", "admin@example.com", "", "", "ADMIN");
        when(userRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(admin));
        mockMvc.perform(get("/api/passenger/trips/search").param("origin", "A")
                .param("destination", "B")).andExpect(status().isForbidden());
    }

    private ScheduledTrip visibleTrip(Long id, LocalDateTime departure) {
        com.yatayat.backend.entity.Route route = new com.yatayat.backend.entity.Route();
        route.setId(id + 100); route.setCode("KTM-PKR"); route.setName("Kathmandu to Pokhara");
        route.setOrigin("Kathmandu"); route.setDestination("Pokhara");
        route.setEstimatedDurationMinutes(360); route.setStatus(RouteStatus.ACTIVE);
        User operatorUser = new User("Operator", "safe@example.com", "", "", "OPERATOR");
        TransportOperator operator = new TransportOperator(); operator.setId(id + 200);
        operator.setUser(operatorUser); operator.setName("Safe Travels");
        operator.setVerificationStatus(OperatorVerificationStatus.APPROVED);
        Bus bus = new Bus(); bus.setId(id + 300); bus.setBusNumber("BA-1-KHA-1000");
        bus.setBusName("Tourist Coach"); bus.setStatus(BusStatus.APPROVED); bus.setSeatCapacity(40);
        bus.setPermitExpiryDate(departure.toLocalDate().plusYears(1));
        bus.setInsuranceExpiryDate(departure.toLocalDate().plusYears(1));
        User driverUser = new User("Private Driver", "driver@example.com", "", "", "DRIVER");
        DriverProfile driver = new DriverProfile(driverUser); driver.setId(id + 400);
        driver.setVerificationStatus(DriverVerificationStatus.APPROVED);
        driver.setLicenseExpiryDate(departure.toLocalDate().plusYears(1));
        ScheduledTrip result = new ScheduledTrip(); result.setId(id); result.setRoute(route);
        result.setOperator(operator); result.setBus(bus); result.setDriver(driver);
        result.setDepartureAt(departure); result.setEstimatedArrivalAt(departure.plusHours(6));
        result.setFare(new BigDecimal("1500.00")); result.setSeatCapacitySnapshot(40);
        result.setStatus(TripStatus.SCHEDULED); result.setBoardingNotes("Board at gate 2");
        return result;
    }
}
