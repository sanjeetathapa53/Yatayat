package com.yatayat.backend.route;

import com.yatayat.backend.config.SecurityConfig;
import com.yatayat.backend.controller.AdminRouteController;
import com.yatayat.backend.controller.OperatorRouteController;
import com.yatayat.backend.entity.*;
import com.yatayat.backend.repository.RouteRepository;
import com.yatayat.backend.repository.TransportOperatorRepository;
import com.yatayat.backend.repository.UserRepository;
import com.yatayat.backend.service.RouteService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
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
    private UserRepository userRepository;
    @MockitoBean
    private TransportOperatorRepository operatorRepository;

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
