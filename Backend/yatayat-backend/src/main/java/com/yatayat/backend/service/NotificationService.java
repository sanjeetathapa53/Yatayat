package com.yatayat.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yatayat.backend.dto.NotificationResponse;
import com.yatayat.backend.dto.NotificationUnreadCountResponse;
import com.yatayat.backend.entity.*;
import com.yatayat.backend.repository.NotificationRepository;
import com.yatayat.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class NotificationService {
    private static final TypeReference<Map<String, String>> METADATA_TYPE = new TypeReference<>() {};

    private final NotificationRepository notifications;
    private final UserRepository users;
    private final ObjectMapper objectMapper;

    public NotificationService(NotificationRepository notifications,
                               UserRepository users,
                               ObjectMapper objectMapper) {
        this.notifications = notifications;
        this.users = users;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> list(String email) {
        User passenger = requirePassenger(email);
        return notifications.findByRecipientOrderByCreatedAtDesc(passenger)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public NotificationUnreadCountResponse unreadCount(String email) {
        return new NotificationUnreadCountResponse(
                notifications.countByRecipientAndReadAtIsNull(requirePassenger(email)));
    }

    @Transactional
    public NotificationResponse markRead(String email, Long notificationId) {
        User passenger = requirePassenger(email);
        Notification notification = notifications.findByIdAndRecipient(notificationId, passenger)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Notification not found."));
        if (notification.getReadAt() == null) {
            notification.setReadAt(LocalDateTime.now());
        }
        return toResponse(notifications.save(notification));
    }

    @Transactional
    public NotificationUnreadCountResponse markAllRead(String email) {
        User passenger = requirePassenger(email);
        notifications.markAllRead(passenger, LocalDateTime.now());
        return new NotificationUnreadCountResponse(0);
    }

    @Transactional
    public void create(User recipient, NotificationType type,
                       String referenceId, Map<String, String> metadata) {
        if (recipient == null || type == null || referenceId == null || referenceId.isBlank()) return;
        String cleanReference = referenceId.trim();
        if (notifications.existsByRecipientAndTypeAndReferenceId(
                recipient, type, cleanReference)) return;

        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setType(type);
        notification.setReferenceId(cleanReference);
        notification.setMetadataJson(writeMetadata(metadata));
        notification.setCreatedAt(LocalDateTime.now());
        notifications.save(notification);
    }

    public void bookingConfirmed(PassengerTripBooking booking) {
        create(booking.getPassenger(), NotificationType.BOOKING_CONFIRMED,
                booking.getBookingReference(), bookingMetadata(booking));
    }

    public void bookingCancelled(PassengerTripBooking booking) {
        create(booking.getPassenger(), NotificationType.BOOKING_CANCELLED,
                booking.getBookingReference(), bookingMetadata(booking));
    }

    public void paymentSuccessful(PassengerTripBooking booking, Payment payment) {
        Map<String, String> metadata = bookingMetadata(booking);
        metadata.put("amount", payment.getAmount().toPlainString());
        metadata.put("provider", payment.getPaymentMethod().name());
        create(booking.getPassenger(), NotificationType.PAYMENT_SUCCESSFUL,
                booking.getBookingReference(), metadata);
    }

    public void ticketQrGenerated(PassengerTripBooking booking, Ticket ticket) {
        Map<String, String> metadata = bookingMetadata(booking);
        metadata.put("bookingId", String.valueOf(booking.getId()));
        metadata.put("bookingNumber", booking.getBookingReference());
        metadata.put("ticketNumber", ticket.getTicketNumber());
        metadata.put("scheduledTripId", String.valueOf(booking.getScheduledTrip().getId()));
        metadata.put("actionPath", "/passenger/bookings/" + booking.getBookingReference());
        create(booking.getPassenger(), NotificationType.TICKET_QR_GENERATED,
                "TICKET_QR_GENERATED:" + ticket.getTicketNumber(), metadata);
    }

    public void localFareQrGenerated(LocalFarePass pass) {
        Map<String, String> metadata = localFareMetadata(pass);
        create(pass.getPassenger(), NotificationType.LOCAL_FARE_QR_GENERATED,
                "LOCAL_FARE_QR_GENERATED:" + pass.getPassNumber(), metadata);
    }

    public void ticketUsed(Ticket ticket) {
        PassengerTripBooking booking = ticket.getBooking();
        Map<String, String> metadata = bookingMetadata(booking);
        metadata.put("bookingId", String.valueOf(booking.getId()));
        metadata.put("ticketNumber", ticket.getTicketNumber());
        metadata.put("scheduledTripId", String.valueOf(booking.getScheduledTrip().getId()));
        metadata.put("scannedAt", String.valueOf(ticket.getUsedAt()));
        metadata.put("actionPath", "/passenger/bookings/" + booking.getBookingReference());
        create(booking.getPassenger(), NotificationType.TICKET_USED,
                "TICKET_USED:" + ticket.getTicketNumber(), metadata);
    }

    public void localFarePassUsed(LocalFarePass pass) {
        Map<String, String> metadata = localFareMetadata(pass);
        metadata.put("localServiceRunId",
                String.valueOf(pass.getValidatedLocalServiceRun().getId()));
        metadata.put("scannedAt", String.valueOf(pass.getUsedAt()));
        create(pass.getPassenger(), NotificationType.LOCAL_FARE_PASS_USED,
                "LOCAL_FARE_PASS_USED:" + pass.getPassNumber(), metadata);
    }

    public void walletTopUpSuccessful(WalletTopUp topUp) {
        create(topUp.getPassenger(), NotificationType.WALLET_TOP_UP_SUCCESSFUL,
                topUp.getTopUpReference(), Map.of(
                        "amount", topUp.getAmount().toPlainString(),
                        "provider", topUp.getPaymentMethod().name()));
    }

    private Map<String, String> bookingMetadata(PassengerTripBooking booking) {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("bookingReference", booking.getBookingReference());
        if (booking.getScheduledTrip() != null && booking.getScheduledTrip().getRoute() != null) {
            metadata.put("routeName", booking.getScheduledTrip().getRoute().getName());
        }
        return metadata;
    }

    private Map<String, String> localFareMetadata(LocalFarePass pass) {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("passNumber", pass.getPassNumber());
        metadata.put("routeId", String.valueOf(pass.getRoute().getId()));
        metadata.put("boardingStop", pass.getBoardingStopName());
        metadata.put("destinationStop", pass.getDestinationStopName());
        metadata.put("actionPath", "/fare-pass");
        return metadata;
    }

    private String writeMetadata(Map<String, String> metadata) {
        try {
            return objectMapper.writeValueAsString(metadata == null ? Map.of() : metadata);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to store notification metadata.", exception);
        }
    }

    private NotificationResponse toResponse(Notification notification) {
        try {
            Map<String, String> metadata = notification.getMetadataJson() == null
                    ? Map.of()
                    : objectMapper.readValue(notification.getMetadataJson(), METADATA_TYPE);
            return new NotificationResponse(notification.getId(), notification.getType().name(),
                    notification.getReferenceId(), metadata, notification.getCreatedAt(),
                    notification.getReadAt() != null);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to read notification metadata.", exception);
        }
    }

    private User requirePassenger(String email) {
        User user = users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Authenticated user not found."));
        if (!"PASSENGER".equalsIgnoreCase(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Passenger access is required.");
        }
        return user;
    }
}
