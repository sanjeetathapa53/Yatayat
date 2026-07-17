package com.yatayat.backend.route;

import com.yatayat.backend.config.SecurityConfig;
import com.yatayat.backend.controller.PassengerLocalRouteController;
import com.yatayat.backend.entity.*;
import com.yatayat.backend.repository.RouteRepository;
import com.yatayat.backend.repository.UserRepository;
import com.yatayat.backend.service.PassengerLocalRouteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PassengerLocalRouteController.class)
@Import({SecurityConfig.class, PassengerLocalRouteService.class})
class PassengerLocalRouteIntegrationTests {
    @Autowired MockMvc mockMvc;
    @MockitoBean UserRepository userRepository;
    @MockitoBean RouteRepository routeRepository;

    private User passenger;
    private Route local;
    private Route outside;

    @BeforeEach
    void setUp() {
        passenger = new User("Passenger", "passenger@example.com", "9800000000", "encoded", "PASSENGER");
        passenger.setId(1L);
        when(userRepository.findByEmailIgnoreCase("passenger@example.com")).thenReturn(Optional.of(passenger));
        local = route(10L, "LOCAL-10", "Koteshwor", "Kalanki", TripType.LOCAL);
        outside = route(11L, "KTM-PKR", "Koteshwor", "Kalanki", TripType.OUT_OF_VALLEY);
    }

    @Test
    void anonymousSearchIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/passenger/local-routes/search")
                        .param("origin", "Koteshwor").param("destination", "Kalanki"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "driver@example.com", roles = "DRIVER")
    void wrongRoleIsForbidden() throws Exception {
        User driver = new User("Driver", "driver@example.com", "", "encoded", "DRIVER");
        when(userRepository.findByEmailIgnoreCase("driver@example.com")).thenReturn(Optional.of(driver));
        mockMvc.perform(get("/api/passenger/local-routes/search")
                        .param("origin", "Koteshwor").param("destination", "Kalanki"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "passenger@example.com", roles = "PASSENGER")
    void localSearchReturnsLocalOnlyAndExcludesOutOfValley() throws Exception {
        when(routeRepository.findByStatusAndTripTypeAndOriginIgnoreCaseAndDestinationIgnoreCaseOrderByCodeAsc(
                RouteStatus.ACTIVE, TripType.LOCAL, "Koteshwor", "Kalanki"))
                .thenReturn(List.of(local, outside));
        mockMvc.perform(get("/api/passenger/local-routes/search")
                        .param("origin", "Koteshwor").param("destination", "Kalanki"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].tripType").value("LOCAL"))
                .andExpect(jsonPath("$[0].routeCode").value("LOCAL-10"));
    }

    @Test
    @WithMockUser(username = "passenger@example.com", roles = "PASSENGER")
    void reversedEndpointOrderDoesNotMatch() throws Exception {
        when(routeRepository.findByStatusAndTripTypeAndOriginIgnoreCaseAndDestinationIgnoreCaseOrderByCodeAsc(
                RouteStatus.ACTIVE, TripType.LOCAL, "Kalanki", "Koteshwor"))
                .thenReturn(List.of());
        mockMvc.perform(get("/api/passenger/local-routes/search")
                        .param("origin", "Kalanki").param("destination", "Koteshwor"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @WithMockUser(username = "passenger@example.com", roles = "PASSENGER")
    void malformedSearchIsRejected() throws Exception {
        mockMvc.perform(get("/api/passenger/local-routes/search")
                        .param("origin", "Koteshwor").param("destination", "Koteshwor"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Origin and destination must be different."));
    }

    @Test
    @WithMockUser(username = "passenger@example.com", roles = "PASSENGER")
    void passengerCanViewActiveLocalRouteDetails() throws Exception {
        when(routeRepository.findByIdAndStatusAndTripType(10L, RouteStatus.ACTIVE, TripType.LOCAL))
                .thenReturn(Optional.of(local));
        mockMvc.perform(get("/api/passenger/local-routes/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stopSummary[0]").value("Koteshwor"))
                .andExpect(jsonPath("$.stopSummary[1]").value("Kalanki"));
    }

    private Route route(Long id, String code, String origin, String destination, TripType type) {
        Route route = new Route(); route.setId(id); route.setCode(code); route.setName(code + " Route");
        route.setOrigin(origin); route.setDestination(destination); route.setTripType(type);
        route.setStatus(RouteStatus.ACTIVE); route.setDistanceKm(new BigDecimal("12.50"));
        route.setEstimatedDurationMinutes(45); return route;
    }
}
