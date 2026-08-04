package com.yatayat.backend.route;

import com.yatayat.backend.config.SecurityConfig;
import com.yatayat.backend.controller.AdminRouteController;
import com.yatayat.backend.controller.OperatorRouteController;
import com.yatayat.backend.entity.*;
import com.yatayat.backend.repository.RouteRepository;
import com.yatayat.backend.repository.BusStopRepository;
import com.yatayat.backend.repository.RouteStopRepository;
import com.yatayat.backend.repository.TransportOperatorRepository;
import com.yatayat.backend.repository.UserRepository;
import com.yatayat.backend.service.RouteService;
import com.yatayat.backend.service.RouteDeletionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        AdminRouteController.class,
        OperatorRouteController.class
})
@Import({SecurityConfig.class, RouteService.class})
class RouteFoundationIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RouteRepository routeRepository;
    @MockitoBean
    private BusStopRepository busStopRepository;
    @MockitoBean
    private RouteStopRepository routeStopRepository;
    @MockitoBean
    private UserRepository userRepository;
    @MockitoBean
    private TransportOperatorRepository operatorRepository;
    @MockitoBean
    private RouteDeletionService routeDeletionService;

    private User operatorUser;
    private TransportOperator operator;

    @BeforeEach
    void setUp() {
        operatorUser = new User(
                "Operator User", "operator@example.com", "9800000001",
                "encoded", "OPERATOR"
        );
        operatorUser.setId(10L);

        operator = new TransportOperator();
        operator.setId(20L);
        operator.setUser(operatorUser);
        operator.setName("Yatayat Operator");
        operator.setVerificationStatus(OperatorVerificationStatus.APPROVED);
        when(routeStopRepository.findByRouteIdOrderByStopOrderAsc(anyLong())).thenReturn(List.of());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanCreateRoute() throws Exception {
        when(routeRepository.existsByCodeIgnoreCase("KTM-PKR-01"))
                .thenReturn(false);
        when(routeRepository.saveAndFlush(any(Route.class))).thenAnswer(invocation -> {
            Route route = invocation.getArgument(0);
            route.setId(1L);
            return route;
        });

        mockMvc.perform(post("/api/admin/routes")
                        .contentType("application/json")
                        .content(validRequest("KTM-PKR-01")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("KTM-PKR-01"))
                .andExpect(jsonPath("$.tripType").value("OUT_OF_VALLEY"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void duplicateRouteCodeIsRejected() throws Exception {
        when(routeRepository.existsByCodeIgnoreCase("KTM-PKR-01"))
                .thenReturn(true);

        mockMvc.perform(post("/api/admin/routes")
                        .contentType("application/json")
                        .content(validRequest("KTM-PKR-01")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Route code is already registered"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void invalidRouteValuesAreRejected() throws Exception {
        mockMvc.perform(post("/api/admin/routes")
                        .contentType("application/json")
                        .content("""
                                {
                                  "code":"BAD-01",
                                  "name":"Invalid Route",
                                  "origin":"Kathmandu",
                                  "destination":"Pokhara",
                                  "distanceKm":0,
                                  "estimatedDurationMinutes":-1,
                                  "status":"ACTIVE"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanCreateLocalRouteAndReceiveLocalType() throws Exception {
        when(routeRepository.existsByCodeIgnoreCase("KTM-LAL-01")).thenReturn(false);
        when(routeRepository.saveAndFlush(any(Route.class))).thenAnswer(invocation -> {
            Route route = invocation.getArgument(0);
            route.setId(2L);
            return route;
        });

        mockMvc.perform(post("/api/admin/routes")
                        .contentType("application/json")
                        .content(validRequest("KTM-LAL-01", "LOCAL")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tripType").value("LOCAL"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanUpdateOutsideValleyRouteToLocal() throws Exception {
        Route route = route(3L, "KTM-PKR-01", RouteStatus.ACTIVE);
        route.setTripType(TripType.OUT_OF_VALLEY);
        prepareUpdate(route);

        mockMvc.perform(put("/api/admin/routes/3")
                        .contentType("application/json")
                        .content(validRequest("KTM-PKR-01", "LOCAL")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tripType").value("LOCAL"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanUpdateLocalRouteToOutsideValley() throws Exception {
        Route route = route(4L, "KTM-BKT-01", RouteStatus.ACTIVE);
        route.setTripType(TripType.LOCAL);
        prepareUpdate(route);

        mockMvc.perform(put("/api/admin/routes/4")
                        .contentType("application/json")
                        .content(validRequest("KTM-BKT-01", "OUT_OF_VALLEY")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tripType").value("OUT_OF_VALLEY"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updatingLocalRouteWithoutChangingTypeKeepsLocal() throws Exception {
        Route route = route(5L, "KTM-KIR-01", RouteStatus.ACTIVE);
        route.setTripType(TripType.LOCAL);
        prepareUpdate(route);

        mockMvc.perform(put("/api/admin/routes/5")
                        .contentType("application/json")
                        .content(validRequest("KTM-KIR-01", "LOCAL")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tripType").value("LOCAL"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void tripTypeIsRequiredWhenCreatingRoute() throws Exception {
        mockMvc.perform(post("/api/admin/routes")
                        .contentType("application/json")
                        .content("""
                                {"code":"KTM-LAL-01","name":"Local Route","origin":"Kathmandu",
                                 "destination":"Lalitpur","distanceKm":12,"estimatedDurationMinutes":45}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Trip type is required"));
    }

    @Test
    @WithMockUser(username = "operator@example.com", roles = "OPERATOR")
    void approvedOperatorSeesOnlyActiveRoutes() throws Exception {
        Route active = route(1L, "ACTIVE-01", RouteStatus.ACTIVE);
        when(userRepository.findByEmailIgnoreCase("operator@example.com"))
                .thenReturn(Optional.of(operatorUser));
        when(operatorRepository.findByUser(operatorUser))
                .thenReturn(Optional.of(operator));
        when(routeRepository.findByStatusOrderByCodeAsc(RouteStatus.ACTIVE))
                .thenReturn(List.of(active));

        mockMvc.perform(get("/api/operator/routes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("ACTIVE-01"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @WithMockUser(username = "operator@example.com", roles = "OPERATOR")
    void pendingOperatorCannotAccessOperatorRoutes() throws Exception {
        operator.setVerificationStatus(OperatorVerificationStatus.PENDING);
        when(userRepository.findByEmailIgnoreCase("operator@example.com"))
                .thenReturn(Optional.of(operatorUser));
        when(operatorRepository.findByUser(operatorUser))
                .thenReturn(Optional.of(operator));

        mockMvc.perform(get("/api/operator/routes"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PASSENGER")
    void passengerCannotAccessAdminRoutes() throws Exception {
        mockMvc.perform(get("/api/admin/routes"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "DRIVER")
    void driverCannotAccessAdminRoutes() throws Exception {
        mockMvc.perform(get("/api/admin/routes"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminFiltersRoutesByTypeAndStatus() throws Exception {
        Route activeLocal = route(40L, "LOCAL-ACTIVE", RouteStatus.ACTIVE);
        activeLocal.setTripType(TripType.LOCAL);
        Route inactiveLocal = route(41L, "LOCAL-INACTIVE", RouteStatus.INACTIVE);
        inactiveLocal.setTripType(TripType.LOCAL);
        Route outside = route(42L, "OUTSIDE-ACTIVE", RouteStatus.ACTIVE);
        outside.setTripType(TripType.OUT_OF_VALLEY);
        when(routeRepository.findAllByOrderByCodeAsc())
                .thenReturn(List.of(activeLocal, inactiveLocal, outside));

        mockMvc.perform(get("/api/admin/routes").param("type", "LOCAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].tripType").value("LOCAL"));

        mockMvc.perform(get("/api/admin/routes").param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[1].status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCombinesSearchTypeAndStatusFilters() throws Exception {
        Route match = route(43L, "KTM-PKR", RouteStatus.ACTIVE);
        match.setTripType(TripType.OUT_OF_VALLEY);
        Route wrongStatus = route(44L, "PKR-KTM", RouteStatus.INACTIVE);
        wrongStatus.setTripType(TripType.OUT_OF_VALLEY);
        Route wrongType = route(45L, "LOCAL-PKR", RouteStatus.ACTIVE);
        wrongType.setTripType(TripType.LOCAL);
        when(routeRepository.findAllByOrderByCodeAsc())
                .thenReturn(List.of(match, wrongStatus, wrongType));

        mockMvc.perform(get("/api/admin/routes")
                        .param("search", "pokhara")
                        .param("type", "OUT_OF_VALLEY")
                        .param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].code").value("KTM-PKR"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanDeleteUnusedRoute() throws Exception {
        mockMvc.perform(delete("/api/admin/routes/50"))
                .andExpect(status().isNoContent());
        verify(routeDeletionService).deleteUnusedRoute(50L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deletingUnknownRouteReturnsNotFound() throws Exception {
        doThrow(new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND, "Route not found"))
                .when(routeDeletionService).deleteUnusedRoute(999L);

        mockMvc.perform(delete("/api/admin/routes/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Route not found"));
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void operatorCannotDeleteRoute() throws Exception {
        mockMvc.perform(delete("/api/admin/routes/50"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PASSENGER")
    void passengerCannotDeleteRoute() throws Exception {
        mockMvc.perform(delete("/api/admin/routes/50"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "DRIVER")
    void driverCannotDeleteRoute() throws Exception {
        mockMvc.perform(delete("/api/admin/routes/50"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedUserCannotDeleteRoute() throws Exception {
        mockMvc.perform(delete("/api/admin/routes/50"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticatedUsersCannotAccessRouteApis() throws Exception {
        mockMvc.perform(get("/api/admin/routes"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/operator/routes"))
                .andExpect(status().isUnauthorized());
    }

    private String validRequest(String code) {
        return validRequest(code, "OUT_OF_VALLEY");
    }

    private String validRequest(String code, String tripType) {
        return """
                {
                  "code":"%s",
                  "name":"Kathmandu to Pokhara",
                  "origin":"Kathmandu",
                  "destination":"Pokhara",
                  "distanceKm":200.50,
                  "estimatedDurationMinutes":420,
                  "tripType":"%s",
                  "status":"ACTIVE"
                }
                """.formatted(code, tripType);
    }

    private void prepareUpdate(Route route) {
        when(routeRepository.findById(route.getId())).thenReturn(Optional.of(route));
        when(routeRepository.existsByCodeIgnoreCaseAndIdNot(eq(route.getCode()), eq(route.getId())))
                .thenReturn(false);
        when(routeRepository.saveAndFlush(route)).thenReturn(route);
    }

    private Route route(Long id, String code, RouteStatus status) {
        Route route = new Route();
        route.setId(id);
        route.setCode(code);
        route.setName("Kathmandu to Pokhara");
        route.setOrigin("Kathmandu");
        route.setDestination("Pokhara");
        route.setDistanceKm(new BigDecimal("200.50"));
        route.setEstimatedDurationMinutes(420);
        route.setStatus(status);
        return route;
    }
}
