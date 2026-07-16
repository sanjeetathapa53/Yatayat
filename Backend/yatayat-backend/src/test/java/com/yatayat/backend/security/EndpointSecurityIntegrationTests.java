package com.yatayat.backend.security;

import com.yatayat.backend.config.SecurityConfig;
import com.yatayat.backend.controller.*;
import com.yatayat.backend.dto.DriverInvitationRequest;
import com.yatayat.backend.entity.User;
import com.yatayat.backend.entity.Wallet;
import com.yatayat.backend.entity.WalletTransaction;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
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
        BookingController.class,
        DriverApplicationController.class,
        DriverDashboardController.class,
        DriverOperatorInvitationController.class,
        OperatorApplicationController.class,
        OperatorBusController.class,
        OperatorDriverController.class,
        AdminDriverController.class,
        AdminOperatorController.class,
        AdminAuthController.class,
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
    private WalletRepository walletRepository;
    @MockitoBean
    private WalletTransactionRepository walletTransactionRepository;
    @MockitoBean
    private BookingRepository bookingRepository;
    @MockitoBean
    private EmailService emailService;
    @MockitoBean
    private TicketPdfService ticketPdfService;
    @MockitoBean
    private DriverApplicationService driverApplicationService;
    @MockitoBean
    private DriverDashboardService driverDashboardService;
    @MockitoBean
    private DriverOperatorAssociationService associationService;
    @MockitoBean
    private OperatorApplicationService operatorApplicationService;
    @MockitoBean
    private OperatorBusService operatorBusService;
    @MockitoBean
    private AdminDriverService adminDriverService;

    private User passengerA;

    @BeforeEach
    void setUp() {
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
    void anonymousUserIsDeniedFromDriverEndpoint() throws Exception {
        mockMvc.perform(get("/api/driver/operator-invitations"))
                .andExpect(status().isUnauthorized());
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
    @WithMockUser(username = "passenger-a@example.com", roles = "PASSENGER")
    void passengerCannotViewAnotherPassengersBooking() throws Exception {
        when(userRepository.findByEmailIgnoreCase("passenger-a@example.com"))
                .thenReturn(Optional.of(passengerA));
        when(bookingRepository.findByIdAndPassenger(20L, passengerA))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/bookings/20/ticket-pdf"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "passenger-a@example.com", roles = "PASSENGER")
    void passengerCannotCancelAnotherPassengersBooking() throws Exception {
        when(userRepository.findByEmailIgnoreCase("passenger-a@example.com"))
                .thenReturn(Optional.of(passengerA));
        when(bookingRepository.findByIdAndPassenger(20L, passengerA))
                .thenReturn(Optional.empty());

        mockMvc.perform(put("/api/bookings/20/cancel"))
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

    private Wallet passengerWallet() {
        Wallet wallet = new Wallet(passengerA);
        wallet.setId(10L);
        return wallet;
    }
}
