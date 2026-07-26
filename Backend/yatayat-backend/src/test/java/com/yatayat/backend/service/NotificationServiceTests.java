package com.yatayat.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yatayat.backend.entity.Notification;
import com.yatayat.backend.entity.NotificationType;
import com.yatayat.backend.entity.User;
import com.yatayat.backend.repository.NotificationRepository;
import com.yatayat.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTests {
    @Mock private NotificationRepository notifications;
    @Mock private UserRepository users;

    private NotificationService service;
    private User passenger;

    @BeforeEach
    void setUp() {
        service = new NotificationService(notifications, users, new ObjectMapper());
        passenger = new User("Passenger", "passenger@example.com",
                "9800000000", "encoded", "PASSENGER");
    }

    @Test
    void createsStructuredNotificationWithoutRenderedTextAndDeduplicatesIt() {
        when(notifications.existsByRecipientAndTypeAndReferenceId(
                passenger, NotificationType.BOOKING_CONFIRMED, "BOOK-1"))
                .thenReturn(false, true);

        service.create(passenger, NotificationType.BOOKING_CONFIRMED,
                " BOOK-1 ", Map.of("bookingReference", "BOOK-1"));
        service.create(passenger, NotificationType.BOOKING_CONFIRMED,
                "BOOK-1", Map.of("bookingReference", "BOOK-1"));

        verify(notifications, times(1)).save(argThat(notification ->
                notification.getType() == NotificationType.BOOKING_CONFIRMED
                        && notification.getReferenceId().equals("BOOK-1")
                        && notification.getMetadataJson().contains("bookingReference")));
    }

    @Test
    void unreadCountAndMarkAllAreScopedToAuthenticatedPassenger() {
        when(users.findByEmailIgnoreCase(passenger.getEmail())).thenReturn(Optional.of(passenger));
        when(notifications.countByRecipientAndReadAtIsNull(passenger)).thenReturn(3L);

        assertThat(service.unreadCount(passenger.getEmail()).unreadCount()).isEqualTo(3);
        assertThat(service.markAllRead(passenger.getEmail()).unreadCount()).isZero();

        verify(notifications).markAllRead(eq(passenger), any());
    }

    @Test
    void cannotReadAnotherPassengersNotification() {
        when(users.findByEmailIgnoreCase(passenger.getEmail())).thenReturn(Optional.of(passenger));
        when(notifications.findByIdAndRecipient(99L, passenger)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markRead(passenger.getEmail(), 99L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404 NOT_FOUND");
    }

    @Test
    void nonPassengerIsRejected() {
        User driver = new User("Driver", "driver@example.com",
                "9800000001", "encoded", "DRIVER");
        when(users.findByEmailIgnoreCase(driver.getEmail())).thenReturn(Optional.of(driver));

        assertThatThrownBy(() -> service.list(driver.getEmail()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403 FORBIDDEN");
        verify(notifications, never()).findByRecipientOrderByCreatedAtDesc(any());
    }
}
