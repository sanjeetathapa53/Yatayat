package com.yatayat.backend.security;

import com.yatayat.backend.config.SecurityConfig;
import com.yatayat.backend.controller.*;
import com.yatayat.backend.dto.DriverInvitationRequest;
import com.yatayat.backend.entity.User;
import com.yatayat.backend.repository.*;
import com.yatayat.backend.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        WalletController.class,
        BookingController.class,
        DriverApplicationController.class,
        DriverDashboardController.class,
        DriverOperatorInvitationController.class,
        OperatorApplicationController.class,
        OperatorBusController.class,
        OperatorDriverController.class,
        AdminDriverController.class
})
@Import({SecurityConfig.class, AuthenticatedUserService.class})
class EndpointSecurityIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

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
}
