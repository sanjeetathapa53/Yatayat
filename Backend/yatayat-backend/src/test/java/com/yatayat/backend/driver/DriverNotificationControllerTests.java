package com.yatayat.backend.driver;

import com.yatayat.backend.config.SecurityConfig;
import com.yatayat.backend.controller.DriverNotificationController;
import com.yatayat.backend.dto.DriverNotificationResponse;
import com.yatayat.backend.dto.DriverNotificationUnreadCountResponse;
import com.yatayat.backend.service.DriverNotificationService;
import com.yatayat.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DriverNotificationController.class)
@Import(SecurityConfig.class)
class DriverNotificationControllerTests {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private DriverNotificationService service;
    @MockitoBean private UserRepository userRepository;

    @Test
    void anonymousRequestIsDenied() throws Exception {
        mockMvc.perform(get("/api/driver/notifications")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "PASSENGER")
    void passengerRequestIsDenied() throws Exception {
        mockMvc.perform(get("/api/driver/notifications")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void operatorRequestIsDenied() throws Exception {
        mockMvc.perform(get("/api/driver/notifications")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminRequestIsDenied() throws Exception {
        mockMvc.perform(get("/api/driver/notifications")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "driver@example.com", roles = "DRIVER")
    void driverCanReadAndMarkNotifications() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        when(service.list("driver@example.com", true)).thenReturn(List.of(
                new DriverNotificationResponse(7L, "SCHEDULED_TRIP_ASSIGNED", "Trip assigned",
                        "A trip was assigned.", "SCHEDULED_TRIP", "11", false, null, now)));
        when(service.unreadCount("driver@example.com"))
                .thenReturn(new DriverNotificationUnreadCountResponse(1));
        when(service.markAllRead("driver@example.com"))
                .thenReturn(new DriverNotificationUnreadCountResponse(0));

        mockMvc.perform(get("/api/driver/notifications").param("unreadOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].relatedEntityId").value("11"));
        mockMvc.perform(get("/api/driver/notifications/unread-count"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.unreadCount").value(1));
        mockMvc.perform(put("/api/driver/notifications/read-all"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.unreadCount").value(0));

        verify(service).list("driver@example.com", true);
        verify(service).markAllRead("driver@example.com");
    }
}
