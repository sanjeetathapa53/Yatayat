package com.yatayat.backend.security;

import com.yatayat.backend.config.SecurityConfig;
import com.yatayat.backend.controller.*;
import com.yatayat.backend.dto.DriverInvitationRequest;
import com.yatayat.backend.dto.DriverLocalFarePassValidationResponse;
import com.yatayat.backend.dto.DriverTicketValidationResponse;
import com.yatayat.backend.dto.TripLocationResponse;
import com.yatayat.backend.dto.PassengerTripLocationResponse;
import com.yatayat.backend.dto.AdminDashboardAnalyticsResponse;
import com.yatayat.backend.entity.User;
import com.yatayat.backend.entity.Wallet;
import com.yatayat.backend.entity.WalletTransaction;
import com.yatayat.backend.entity.AuthenticationProvider;
import com.yatayat.backend.repository.*;
import com.yatayat.backend.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@WebMvcTest(controllers = {
        WalletController.class,
        DriverApplicationController.class,
        DriverDashboardController.class,
        DriverOperatorInvitationController.class,
        OperatorApplicationController.class,
        OperatorBusController.class,
        OperatorDriverController.class,
        AdminDriverController.class,
        AdminOperatorController.class,
        AdminAuthController.class,
        DriverTicketController.class,
        DriverLocalFarePassController.class,
        PassengerLocalFarePassController.class,
        DriverTripOperationController.class,
        PassengerLiveTrackingController.class,
        PassengerLocalLiveServiceController.class,
        OperatorLiveFleetController.class,
        OperatorLocalLiveFleetController.class,
        AdminLiveFleetController.class,
        AdminDashboardAnalyticsController.class,
        AuthController.class
})
@Import({SecurityConfig.class, AuthenticatedUserService.class, SessionLogoutService.class})
class EndpointSecurityIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private OtpVerificationService otpVerificationService;
    @MockitoBean
    private WalletRepository walletRepository;
    @MockitoBean
    private WalletTransactionRepository walletTransactionRepository;
    @MockitoBean
    private EmailService emailService;
    @MockitoBean
    private DriverApplicationService driverApplicationService;
    @MockitoBean
    private DriverDashboardService driverDashboardService;
    @MockitoBean
    private DriverOperatorAssociationService associationService;
    @MockitoBean
    private DriverTicketValidationService driverTicketValidationService;
    @MockitoBean
    private DriverLocalFarePassValidationService driverLocalFarePassValidationService;
    @MockitoBean
    private LocalFarePassService localFarePassService;
    @MockitoBean
    private TripOperationService tripOperationService;
    @MockitoBean
    private TripLocationService tripLocationService;
    @MockitoBean
    private PassengerLiveTrackingService passengerLiveTrackingService;
    @MockitoBean
    private PassengerLocalLiveServiceService passengerLocalLiveServiceService;
    @MockitoBean
    private OperatorLiveFleetService operatorLiveFleetService;
    @MockitoBean
    private OperatorLocalLiveFleetService operatorLocalLiveFleetService;
    @MockitoBean
    private AdminLiveFleetService adminLiveFleetService;
    @MockitoBean
    private AdminDashboardAnalyticsService adminDashboardAnalyticsService;
    @MockitoBean
    private OperatorApplicationService operatorApplicationService;
    @MockitoBean
    private OperatorBusService operatorBusService;
    @MockitoBean
    private AdminDriverService adminDriverService;

    private User passengerA;

    @BeforeEach
    void setUp() {
        when(otpVerificationService.normalizeEmail(any()))
                .thenAnswer(call -> call.getArgument(0));
        passengerA = new User(
                "Passenger A",
                "passenger-a@example.com",
                "9800000001",
                "encoded",
                "PASSENGER"
        );
        passengerA.setId(1L);
    }

    @Test
    void anonymousUserIsDeniedFromPassengerEndpoint() throws Exception {
        mockMvc.perform(get("/api/wallet/balance/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anonymousUserCannotRestoreSession() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void passengerDriverAndOperatorSessionsRestoreSafeIdentity() throws Exception {
        for (String role : List.of("PASSENGER", "DRIVER", "OPERATOR")) {
            String email = role.toLowerCase() + "@example.com";
            User user = new User(role + " User", email, "9800000000",
                    passwordEncoder.encode("test-password"), role);
            user.setId((long) role.length());
            when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
            when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));

            MvcResult login = mockMvc.perform(post("/api/auth/login")
                            .contentType("application/json")
                            .content("{\"email\":\"" + email + "\",\"password\":\"test-password\"}"))
                    .andExpect(status().isOk())
                    .andReturn();

            MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
            assertNotNull(session);
            mockMvc.perform(get("/api/auth/me").session(session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value(email))
                    .andExpect(jsonPath("$.role").value(role))
                    .andExpect(jsonPath("$.password").doesNotExist())
                    .andExpect(jsonPath("$.otp").doesNotExist());
        }
    }

    @Test
    void adminSessionRestoresSafeIdentity() throws Exception {
        User admin = new User("Administrator", "restore-admin@example.com", "",
                passwordEncoder.encode("test-admin-password"), "ADMIN");
        admin.setId(99L);
        when(userRepository.findByEmail("restore-admin@example.com")).thenReturn(Optional.of(admin));
        when(userRepository.findByEmailIgnoreCase("restore-admin@example.com")).thenReturn(Optional.of(admin));

        MvcResult login = mockMvc.perform(post("/api/admin/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"restore-admin@example.com\",\"password\":\"test-admin-password\"}"))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void passengerLoginCreatesRolePassengerSession() throws Exception {
        MockHttpSession session = loginPassenger();
        SecurityContext context = (SecurityContext) session.getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY
        );

        assertNotNull(context);
        assertTrue(context.getAuthentication().getAuthorities().stream()
                .anyMatch(authority -> "ROLE_PASSENGER".equals(authority.getAuthority())));
    }

    @Test
    void registrationCreatesRolePassengerSession() throws Exception {
        when(userRepository.existsByEmailIgnoreCase("new-passenger@example.com"))
                .thenReturn(false);

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content("{\"fullName\":\"New Passenger\",\"email\":\"new-passenger@example.com\",\"phone\":\"9800000003\",\"password\":\"test-password\",\"role\":\"PASSENGER\"}"))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertNotNull(session);
        SecurityContext context = (SecurityContext) session.getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY
        );
        assertNotNull(context);
        assertTrue(context.getAuthentication().getAuthorities().stream()
                .anyMatch(authority -> "ROLE_PASSENGER".equals(authority.getAuthority())));
    }

    @Test
    void googleAccountCannotUsePasswordLogin() throws Exception {
        User googleUser = new User(
                "Google Passenger", "google@example.com", "", null, "PASSENGER");
        googleUser.setAuthenticationProvider(AuthenticationProvider.GOOGLE);
        when(userRepository.findByEmail("google@example.com"))
                .thenReturn(Optional.of(googleUser));

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("""
                                {"email":"google@example.com","password":"irrelevant"}
                                """))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        "This account uses Google Sign-In. Please continue with Google."));
    }

    @Test
    void loggedInPassengerCanAccessOwnWalletBalance() throws Exception {
        MockHttpSession session = loginPassenger();
        Wallet wallet = passengerWallet();
        wallet.setBalance(125.0);
        when(walletRepository.findByUser(passengerA)).thenReturn(Optional.of(wallet));

        mockMvc.perform(get("/api/wallet/balance/1").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string("125.0"));
    }

    @Test
    void loggedInPassengerCanAccessOwnWalletPinStatus() throws Exception {
        MockHttpSession session = loginPassenger();
        Wallet wallet = passengerWallet();
        wallet.setWalletPin(passwordEncoder.encode("1234"));
        when(walletRepository.findByUser(passengerA)).thenReturn(Optional.of(wallet));

        mockMvc.perform(get("/api/wallet/pin-status/1").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string("PIN_SET"));
    }

    @Test
    void loggedInPassengerCanAccessOwnWalletHistory() throws Exception {
        MockHttpSession session = loginPassenger();
        Wallet wallet = passengerWallet();
        when(walletRepository.findByUser(passengerA)).thenReturn(Optional.of(wallet));
        when(walletTransactionRepository.findByWalletOrderByTransactionDateDesc(wallet))
                .thenReturn(List.of(new WalletTransaction(
                        wallet, "TOPUP", 100.0, "SUCCESS", "KHALTI"
                )));

        mockMvc.perform(get("/api/wallet/history/1").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("TOPUP"));
    }

    @Test
    void loggedInPassengerCannotAccessAnotherPassengersWallet() throws Exception {
        MockHttpSession session = loginPassenger();

        mockMvc.perform(get("/api/wallet/balance/2").session(session))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "DRIVER")
    void driverCannotAccessPassengerWallet() throws Exception {
        mockMvc.perform(get("/api/wallet/balance/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void operatorCannotAccessPassengerWallet() throws Exception {
        mockMvc.perform(get("/api/wallet/balance/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCannotAccessPassengerWallet() throws Exception {
        mockMvc.perform(get("/api/wallet/balance/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PASSENGER")
    void passengerCannotValidateDriverTicket() throws Exception {
        mockMvc.perform(post("/api/driver/tickets/validate")
                        .contentType("application/json")
                        .content("{\"qrPayload\":\"{}\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PASSENGER")
    void passengerCannotStartDriverTrip() throws Exception {
        mockMvc.perform(post("/api/driver/trips/50/start"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void operatorCannotValidateDriverTicket() throws Exception {
        mockMvc.perform(post("/api/driver/tickets/validate")
                        .contentType("application/json")
                        .content("{\"qrPayload\":\"{}\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void operatorCannotStartDriverTrip() throws Exception {
        mockMvc.perform(post("/api/driver/trips/50/start"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCannotValidateDriverTicket() throws Exception {
        mockMvc.perform(post("/api/driver/tickets/validate")
                        .contentType("application/json")
                        .content("{\"qrPayload\":\"{}\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCannotStartDriverTrip() throws Exception {
        mockMvc.perform(post("/api/driver/trips/50/start"))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousUserIsDeniedFromDriverEndpoint() throws Exception {
        mockMvc.perform(get("/api/driver/operator-invitations"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anonymousUserIsDeniedFromDriverTicketValidationEndpoint() throws Exception {
        mockMvc.perform(post("/api/driver/tickets/validate")
                        .contentType("application/json")
                        .content("{\"qrPayload\":\"{}\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anonymousUserIsDeniedFromDriverTripStartEndpoint() throws Exception {
        mockMvc.perform(post("/api/driver/trips/50/start"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anonymousUserCannotUpdateTripLocation() throws Exception {
        mockMvc.perform(put("/api/driver/trips/50/location")
                        .contentType("application/json")
                        .content("{\"latitude\":27.7,\"longitude\":85.3}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "PASSENGER")
    void nonDriverCannotUpdateTripLocation() throws Exception {
        mockMvc.perform(put("/api/driver/trips/50/location")
                        .contentType("application/json")
                        .content("{\"latitude\":27.7,\"longitude\":85.3}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "driver@example.com", roles = "DRIVER")
    void validTripLocationUpdateUsesAuthenticatedDriver() throws Exception {
        when(tripLocationService.update(
                org.mockito.ArgumentMatchers.eq("driver@example.com"),
                org.mockito.ArgumentMatchers.eq(50L),
                any(com.yatayat.backend.dto.TripLocationUpdateRequest.class)
        )).thenReturn(new TripLocationResponse(
                50L, 27.7, 85.3, 5.0, 20.0, 90.0,
                LocalDateTime.of(2026, 7, 22, 10, 0)
        ));

        mockMvc.perform(put("/api/driver/trips/50/location")
                        .contentType("application/json")
                        .content("{\"latitude\":27.7,\"longitude\":85.3," +
                                "\"accuracy\":5,\"speed\":20,\"heading\":90}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tripId").value(50))
                .andExpect(jsonPath("$.latitude").value(27.7));

        verify(tripLocationService).update(
                org.mockito.ArgumentMatchers.eq("driver@example.com"),
                org.mockito.ArgumentMatchers.eq(50L),
                any(com.yatayat.backend.dto.TripLocationUpdateRequest.class)
        );
    }

    @Test
    @WithMockUser(username = "driver@example.com", roles = "DRIVER")
    void invalidLatitudeAndLongitudeReturnBadRequest() throws Exception {
        for (String body : List.of(
                "{\"latitude\":90.1,\"longitude\":85.3}",
                "{\"latitude\":27.7,\"longitude\":-180.1}"
        )) {
            mockMvc.perform(put("/api/driver/trips/50/location")
                            .contentType("application/json")
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }
        verifyNoInteractions(tripLocationService);
    }

    @Test
    void anonymousUserCannotReadPassengerLiveLocations() throws Exception {
        mockMvc.perform(get("/api/passenger/live-trips"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "DRIVER")
    void driverCannotReadPassengerLiveLocations() throws Exception {
        mockMvc.perform(get("/api/passenger/live-trips"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PASSENGER")
    void passengerCanOnlyReadLiveLocations() throws Exception {
        when(passengerLiveTrackingService.activeLocations()).thenReturn(List.of(
                new PassengerTripLocationResponse(
                        50L,
                        new PassengerTripLocationResponse.BusInfo(4L, "BA 1 PA 1234", "Green Line"),
                        3L, "Ring Road", "Kalanki", "Koteshwor",
                        27.7, 85.3, 10.0, 90.0,
                        LocalDateTime.of(2026, 7, 22, 12, 0),
                        com.yatayat.backend.entity.TripStatus.IN_PROGRESS
                )
        ));

        mockMvc.perform(get("/api/passenger/live-trips"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tripId").value(50))
                .andExpect(jsonPath("$[0].bus.number").value("BA 1 PA 1234"));

        mockMvc.perform(put("/api/passenger/live-trips/50")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void anonymousUserCannotReadPassengerLocalLiveServices() throws Exception {
        mockMvc.perform(get("/api/passenger/local-live-services"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "DRIVER")
    void driverCannotReadPassengerLocalLiveServices() throws Exception {
        mockMvc.perform(get("/api/passenger/local-live-services"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PASSENGER")
    void passengerCanReadLocalLiveServices() throws Exception {
        when(passengerLocalLiveServiceService.activeServices(null)).thenReturn(List.of());

        mockMvc.perform(get("/api/passenger/local-live-services"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void anonymousUserCannotReadOperatorLiveFleet() throws Exception {
        mockMvc.perform(get("/api/operator/live-fleet"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anonymousUserCannotReadOperatorLocalLiveFleet() throws Exception {
        mockMvc.perform(get("/api/operator/local-live-fleet"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "PASSENGER")
    void passengerCannotReadOperatorLocalLiveFleet() throws Exception {
        mockMvc.perform(get("/api/operator/local-live-fleet"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "operator@example.com", roles = "OPERATOR")
    void operatorCanReadOwnLocalLiveFleet() throws Exception {
        when(operatorLocalLiveFleetService.activeFleet("operator@example.com"))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/operator/local-live-fleet"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    @WithMockUser(roles = "PASSENGER")
    void passengerCannotReadOperatorLiveFleet() throws Exception {
        mockMvc.perform(get("/api/operator/live-fleet"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "operator@example.com", roles = "OPERATOR")
    void operatorLiveFleetUsesAuthenticatedOperator() throws Exception {
        when(operatorLiveFleetService.activeFleet("operator@example.com")).thenReturn(List.of());

        mockMvc.perform(get("/api/operator/live-fleet"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(operatorLiveFleetService).activeFleet("operator@example.com");
    }

    @Test
    @WithMockUser(roles = "PASSENGER")
    void passengerCannotRemoveOperatorDriver() throws Exception {
        mockMvc.perform(delete("/api/operator/drivers/5"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "operator@example.com", roles = "OPERATOR")
    void operatorDriverRemovalUsesAuthenticatedOperator() throws Exception {
        mockMvc.perform(delete("/api/operator/drivers/5"))
                .andExpect(status().isOk());

        verify(associationService).remove("operator@example.com", 5L);
    }

    @Test
    void anonymousUserCannotReadAdminLiveFleet() throws Exception {
        mockMvc.perform(get("/api/admin/live-fleet"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "PASSENGER")
    void passengerCannotReadAdminLiveFleet() throws Exception {
        mockMvc.perform(get("/api/admin/live-fleet"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "DRIVER")
    void driverCannotReadAdminLiveFleet() throws Exception {
        mockMvc.perform(get("/api/admin/live-fleet"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void operatorCannotReadAdminLiveFleet() throws Exception {
        mockMvc.perform(get("/api/admin/live-fleet"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void authenticatedAdminCanReadAdminLiveFleet() throws Exception {
        when(adminLiveFleetService.activeFleet()).thenReturn(List.of());

        mockMvc.perform(get("/api/admin/live-fleet"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(adminLiveFleetService).activeFleet();
    }

    @Test
    void anonymousUserIsDeniedFromOperatorEndpoint() throws Exception {
        mockMvc.perform(get("/api/operator/buses"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anonymousUserIsDeniedFromAdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/admin/drivers/pending"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "PASSENGER")
    void passengerIsDeniedFromAdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/admin/drivers/pending"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PASSENGER")
    void passengerIsDeniedFromOperatorEndpoint() throws Exception {
        mockMvc.perform(get("/api/operator/buses"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "DRIVER")
    void driverIsDeniedFromAdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/admin/drivers/pending"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void operatorIsDeniedFromAdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/admin/drivers/pending"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void adminCanUseAdminApprovalEndpoint() throws Exception {
        when(adminDriverService.approveApplication(9L))
                .thenReturn(Map.of("success", true));

        mockMvc.perform(put("/api/admin/drivers/9/approve"))
                .andExpect(status().isOk());
    }

    @Test
    void anonymousCannotReadAdminAnalytics() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "PASSENGER")
    void passengerCannotReadAdminAnalytics() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "DRIVER")
    void driverCannotReadAdminAnalytics() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void operatorCannotReadAdminAnalytics() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanReadDashboardAnalytics() throws Exception {
        AdminDashboardAnalyticsResponse.Summary summary =
                new AdminDashboardAnalyticsResponse.Summary(
                        10, 5, 2, 2, 1, 1, 4, 3, 2, 6, 1, 2, 1,
                        1, 1, 1, 8, 2, 5, 1,
                        new BigDecimal("1200.00"), new BigDecimal("500.00"),
                        new BigDecimal("1700.00"));
        when(adminDashboardAnalyticsService.dashboard("LAST_7_DAYS"))
                .thenReturn(new AdminDashboardAnalyticsResponse(
                        "LAST_7_DAYS", summary,
                        List.of(new AdminDashboardAnalyticsResponse.DailyPoint(
                                LocalDate.now(), 1)),
                        List.of(new AdminDashboardAnalyticsResponse.DailyPoint(
                                LocalDate.now(), 2)),
                        new AdminDashboardAnalyticsResponse.TripBreakdown(1, 2),
                        List.of(new AdminDashboardAnalyticsResponse.RecentActivity(
                                "PAYMENT_VERIFIED", "Ticket payment verified",
                                "PAY-1", LocalDateTime.now()))));

        mockMvc.perform(get("/api/admin/analytics/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.totalUsers").value(10))
                .andExpect(jsonPath("$.summary.totalVerifiedPaymentAmount").value(1700.00))
                .andExpect(jsonPath("$.recentActivity[0].title")
                        .value("Ticket payment verified"))
                .andExpect(jsonPath("$.recentActivity[0].email").doesNotExist())
                .andExpect(jsonPath("$.recentActivity[0].password").doesNotExist());
    }

    @Test
    void anonymousCannotReadDetailedAnalytics() throws Exception {
        for (String section : List.of("users", "operations", "bookings", "revenue")) {
            mockMvc.perform(get("/api/admin/analytics/" + section))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    @WithMockUser(roles = "PASSENGER")
    void passengerCannotReadDetailedAnalytics() throws Exception {
        assertDetailedAnalyticsForbidden();
    }

    @Test
    @WithMockUser(roles = "DRIVER")
    void driverCannotReadDetailedAnalytics() throws Exception {
        assertDetailedAnalyticsForbidden();
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void operatorCannotReadDetailedAnalytics() throws Exception {
        assertDetailedAnalyticsForbidden();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanReadEveryDetailedAnalyticsEndpoint() throws Exception {
        for (String section : List.of("users", "operations", "bookings", "revenue")) {
            mockMvc.perform(get("/api/admin/analytics/" + section))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void detailedAnalyticsRejectInvalidRange() throws Exception {
        when(adminDashboardAnalyticsService.users("INVALID"))
                .thenThrow(new IllegalArgumentException(
                        "Range must be LAST_7_DAYS or LAST_30_DAYS."));
        mockMvc.perform(get("/api/admin/analytics/users").param("range", "INVALID"))
                .andExpect(status().isBadRequest());
    }

    private void assertDetailedAnalyticsForbidden() throws Exception {
        for (String section : List.of("users", "operations", "bookings", "revenue")) {
            mockMvc.perform(get("/api/admin/analytics/" + section))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void adminLoginCreatesRoleBearingSessionForProtectedReads() throws Exception {
        User admin = new User(
                "Administrator",
                "admin@example.com",
                "",
                passwordEncoder.encode("test-admin-password"),
                "ADMIN"
        );
        admin.setId(99L);

        when(userRepository.findByEmail("admin@example.com"))
                .thenReturn(Optional.of(admin));
        when(operatorApplicationService.getAllApplications())
                .thenReturn(List.of());
        when(adminDriverService.getPendingApplications())
                .thenReturn(List.of());

        MvcResult loginResult = mockMvc.perform(
                        post("/api/admin/auth/login")
                                .contentType("application/json")
                                .content("{\"email\":\"admin@example.com\",\"password\":\"test-admin-password\"}")
                )
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session = (MockHttpSession)
                loginResult.getRequest().getSession(false);
        assertNotNull(session);

        SecurityContext securityContext = (SecurityContext) session.getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY
        );
        assertNotNull(securityContext);
        assertTrue(securityContext.getAuthentication().getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority())));

        mockMvc.perform(get("/api/admin/operators").session(session))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/drivers/pending").session(session))
                .andExpect(status().isOk());
    }

    @Test
    void userLogoutInvalidatesSessionAndProtectedEndpointRejectsIt() throws Exception {
        User driver = new User(
                "Driver A",
                "driver-a@example.com",
                "9800000002",
                passwordEncoder.encode("test-driver-password"),
                "DRIVER"
        );
        driver.setId(2L);
        when(userRepository.findByEmail("driver-a@example.com"))
                .thenReturn(Optional.of(driver));

        MvcResult loginResult = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType("application/json")
                                .content("{\"email\":\"driver-a@example.com\",\"password\":\"test-driver-password\"}")
                )
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session = (MockHttpSession)
                loginResult.getRequest().getSession(false);
        assertNotNull(session);

        mockMvc.perform(post("/api/auth/logout").session(session))
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge("JSESSIONID", 0))
                .andExpect(jsonPath("$.success").value(true));

        assertTrue(session.isInvalid());
        mockMvc.perform(get("/api/driver/operator-invitations"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminLogoutInvalidatesSessionAndAdminEndpointRejectsIt() throws Exception {
        User admin = new User(
                "Administrator",
                "admin@example.com",
                "",
                passwordEncoder.encode("test-admin-password"),
                "ADMIN"
        );
        admin.setId(99L);
        when(userRepository.findByEmail("admin@example.com"))
                .thenReturn(Optional.of(admin));

        MvcResult loginResult = mockMvc.perform(
                        post("/api/admin/auth/login")
                                .contentType("application/json")
                                .content("{\"email\":\"admin@example.com\",\"password\":\"test-admin-password\"}")
                )
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session = (MockHttpSession)
                loginResult.getRequest().getSession(false);
        assertNotNull(session);

        SecurityContext securityContext = (SecurityContext) session.getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY
        );
        assertNotNull(securityContext);
        assertTrue(securityContext.getAuthentication().getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority())));

        mockMvc.perform(post("/api/admin/auth/logout").session(session))
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge("JSESSIONID", 0))
                .andExpect(jsonPath("$.success").value(true));

        assertTrue(session.isInvalid());
        mockMvc.perform(get("/api/admin/operators"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "passenger-a@example.com", roles = "PASSENGER")
    void passengerCannotViewAnotherPassengersWallet() throws Exception {
        when(userRepository.findByEmailIgnoreCase("passenger-a@example.com"))
                .thenReturn(Optional.of(passengerA));

        mockMvc.perform(get("/api/wallet/balance/2"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "driver-a@example.com", roles = "DRIVER")
    void driverCannotViewAnotherDriversProfile() throws Exception {
        passengerA.setEmail("driver-a@example.com");
        passengerA.setRole("DRIVER");
        when(userRepository.findByEmailIgnoreCase("driver-a@example.com"))
                .thenReturn(Optional.of(passengerA));

        mockMvc.perform(get("/api/drivers/profile/2"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "operator-a@example.com", roles = "OPERATOR")
    void operatorCannotSubmitApplicationForAnotherAccount() throws Exception {
        passengerA.setEmail("operator-a@example.com");
        passengerA.setRole("OPERATOR");
        when(userRepository.findByEmailIgnoreCase("operator-a@example.com"))
                .thenReturn(Optional.of(passengerA));

        mockMvc.perform(post("/api/operators/application")
                        .contentType("application/json")
                        .content("{\"userId\":2}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "operator-a@example.com", roles = "OPERATOR")
    void operatorCannotAccessAnotherOperatorsBus() throws Exception {
        when(operatorBusService.getBus("operator-a@example.com", 50L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/api/operator/buses/50"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "operator-a@example.com", roles = "OPERATOR")
    void operatorInvitationUsesAuthenticatedOperatorIdentity() throws Exception {
        mockMvc.perform(post("/api/operator/driver-invitations")
                        .contentType("application/json")
                        .content("{\"driverId\":7}"))
                .andExpect(status().isCreated());

        verify(associationService).invite(
                org.mockito.ArgumentMatchers.eq("operator-a@example.com"),
                any(DriverInvitationRequest.class)
        );
    }

    @Test
    @WithMockUser(username = "driver-a@example.com", roles = "DRIVER")
    void driverCannotAcceptAnotherDriversInvitation() throws Exception {
        when(associationService.accept("driver-a@example.com", 44L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

        mockMvc.perform(post("/api/driver/operator-invitations/44/accept"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "driver-a@example.com", roles = "DRIVER")
    void driverCannotRejectAnotherDriversInvitation() throws Exception {
        when(associationService.reject("driver-a@example.com", 44L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

        mockMvc.perform(post("/api/driver/operator-invitations/44/reject"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "driver-a@example.com", roles = "DRIVER")
    void driverCanUseDriverTicketValidationEndpoint() throws Exception {
        when(driverTicketValidationService.validate("driver-a@example.com", "{}"))
                .thenReturn(new DriverTicketValidationResponse(
                        "VALID",
                        "Boarding confirmed.",
                        "YT-TKT-20260718-ABC123",
                        "Passenger A",
                        new DriverTicketValidationResponse.RouteSummary("Kathmandu", "Pokhara"),
                        List.of("1A"),
                        java.time.LocalDateTime.of(2026, 7, 18, 18, 30),
                        "TRIP-50"
                ));

        mockMvc.perform(post("/api/driver/tickets/validate")
                        .contentType("application/json")
                        .content("{\"qrPayload\":\"{}\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("VALID"))
                .andExpect(jsonPath("$.ticketNumber").value("YT-TKT-20260718-ABC123"));
    }

    @Test
    @WithMockUser(username = "driver-a@example.com", roles = "DRIVER")
    void driverTicketRequestValidationReturnsControlledErrors() throws Exception {
        mockMvc.perform(post("/api/driver/tickets/validate")
                        .contentType("application/json")
                        .content("{\"qrPayload\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.result").value("INVALID_QR"))
                .andExpect(jsonPath("$.success").value(false));

        String oversized = "x".repeat(
                DriverTicketValidationService.MAX_QR_PAYLOAD_LENGTH + 1);
        mockMvc.perform(post("/api/driver/tickets/validate")
                        .contentType("application/json")
                        .content("{\"qrPayload\":\"" + oversized + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.result").value("INVALID_QR"));

        mockMvc.perform(post("/api/driver/tickets/validate")
                        .contentType("application/json")
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.result").value("INVALID_QR"));
    }

    private MockHttpSession loginPassenger() throws Exception {
        passengerA.setPassword(passwordEncoder.encode("test-passenger-password"));
        when(userRepository.findByEmail("passenger-a@example.com"))
                .thenReturn(Optional.of(passengerA));
        when(userRepository.findByEmailIgnoreCase("passenger-a@example.com"))
                .thenReturn(Optional.of(passengerA));

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"passenger-a@example.com\",\"password\":\"test-passenger-password\"}"))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertNotNull(session);
        return session;
    }

    @Test
    void anonymousUserIsDeniedFromLocalFarePassEndpoints() throws Exception {
        mockMvc.perform(get("/api/passenger/local-fare-passes"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/driver/local-fare-passes/validate")
                        .contentType("application/json")
                        .content("{\"qrPayload\":\"{}\"}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(localFarePassService, driverLocalFarePassValidationService);
    }

    @Test
    @WithMockUser(username = "passenger@example.com", roles = "PASSENGER")
    void passengerCanListOwnLocalFarePassesButCannotValidateThem() throws Exception {
        when(localFarePassService.list("passenger@example.com")).thenReturn(List.of());
        mockMvc.perform(get("/api/passenger/local-fare-passes"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
        mockMvc.perform(post("/api/driver/local-fare-passes/validate")
                        .contentType("application/json")
                        .content("{\"qrPayload\":\"{}\"}"))
                .andExpect(status().isForbidden());
        verify(localFarePassService).list("passenger@example.com");
    }

    @Test
    @WithMockUser(username = "driver@example.com", roles = "DRIVER")
    void driverCanValidateLocalFarePassButCannotReadPassengerPasses() throws Exception {
        when(driverLocalFarePassValidationService.validate("driver@example.com", "{}"))
                .thenReturn(new DriverLocalFarePassValidationResponse(
                        "VALID", "Local fare confirmed.", "YT-LFP-1", "Passenger",
                        "Stop A", "Stop B", java.math.BigDecimal.TEN,
                        LocalDateTime.now(), 20L));
        mockMvc.perform(post("/api/driver/local-fare-passes/validate")
                        .contentType("application/json")
                        .content("{\"qrPayload\":\"{}\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("VALID"));
        mockMvc.perform(get("/api/passenger/local-fare-passes"))
                .andExpect(status().isForbidden());
    }

    private Wallet passengerWallet() {
        Wallet wallet = new Wallet(passengerA);
        wallet.setId(10L);
        return wallet;
    }
}
