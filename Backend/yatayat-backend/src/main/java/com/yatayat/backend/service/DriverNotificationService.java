package com.yatayat.backend.service;

import com.yatayat.backend.dto.*;
import com.yatayat.backend.entity.*;
import com.yatayat.backend.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class DriverNotificationService {
    private final DriverNotificationRepository notifications;
    private final DriverProfileRepository drivers;
    private final UserRepository users;

    public DriverNotificationService(DriverNotificationRepository notifications,
                                     DriverProfileRepository drivers, UserRepository users) {
        this.notifications = notifications;
        this.drivers = drivers;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<DriverNotificationResponse> list(String email, boolean unreadOnly) {
        DriverProfile driver = requireApprovedDriver(email);
        List<DriverNotification> rows = unreadOnly
                ? notifications.findByDriverAndReadAtIsNullOrderByCreatedAtDesc(driver)
                : notifications.findByDriverOrderByCreatedAtDesc(driver);
        return rows.stream().map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public DriverNotificationUnreadCountResponse unreadCount(String email) {
        return new DriverNotificationUnreadCountResponse(
                notifications.countByDriverAndReadAtIsNull(requireApprovedDriver(email)));
    }

    @Transactional
    public DriverNotificationResponse markRead(String email, Long id) {
        DriverProfile driver = requireApprovedDriver(email);
        DriverNotification row = notifications.findByIdAndDriver(id, driver)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Driver notification not found."));
        if (row.getReadAt() == null) row.setReadAt(LocalDateTime.now());
        return response(notifications.save(row));
    }

    @Transactional
    public DriverNotificationUnreadCountResponse markAllRead(String email) {
        DriverProfile driver = requireApprovedDriver(email);
        notifications.markAllRead(driver, LocalDateTime.now());
        return new DriverNotificationUnreadCountResponse(0);
    }

    public void operatorInvitation(DriverOperatorAssociation value) {
        create(value.getDriver(), DriverNotificationType.OPERATOR_INVITATION,
                "Operator invitation", value.getOperator().getName() + " invited you to join their operation.",
                "OPERATOR_ASSOCIATION", value.getId(), key("invited", value.getId()));
    }
    public void operatorInvitationAccepted(DriverOperatorAssociation value) {
        create(value.getDriver(), DriverNotificationType.OPERATOR_INVITATION_ACCEPTED,
                "Invitation accepted", "You are now associated with " + value.getOperator().getName() + ".",
                "OPERATOR_ASSOCIATION", value.getId(), key("accepted", value.getId()));
    }
    public void operatorInvitationRejected(DriverOperatorAssociation value) {
        create(value.getDriver(), DriverNotificationType.OPERATOR_INVITATION_REJECTED,
                "Invitation rejected", "You rejected the invitation from " + value.getOperator().getName() + ".",
                "OPERATOR_ASSOCIATION", value.getId(), key("rejected", value.getId()));
    }
    public void scheduledAssigned(ScheduledTrip value) { scheduled(value, DriverNotificationType.SCHEDULED_TRIP_ASSIGNED, "assigned"); }
    public void scheduledUpdated(ScheduledTrip value) { scheduled(value, DriverNotificationType.SCHEDULED_TRIP_UPDATED, "updated"); }
    public void scheduledCancelled(ScheduledTrip value) { scheduled(value, DriverNotificationType.SCHEDULED_TRIP_CANCELLED, "cancelled"); }
    public void localAssigned(LocalServiceRun value) { local(value, DriverNotificationType.LOCAL_SERVICE_ASSIGNED, "assigned"); }
    public void localUpdated(LocalServiceRun value) { local(value, DriverNotificationType.LOCAL_SERVICE_UPDATED, "updated"); }
    public void localCancelled(LocalServiceRun value) { local(value, DriverNotificationType.LOCAL_SERVICE_CANCELLED, "cancelled"); }

    private void scheduled(ScheduledTrip trip, DriverNotificationType type, String action) {
        String route = routeLabel(trip.getRoute());
        create(trip.getDriver(), type, "Scheduled trip " + action,
                "Your scheduled trip for " + route + " was " + action + ".",
                "SCHEDULED_TRIP", trip.getId(), key(action, trip.getId(), id(trip.getDriver()),
                        id(trip.getBus()), id(trip.getRoute()), trip.getDepartureAt(),
                        trip.getEstimatedArrivalAt(), trip.getStatus(), trip.getFare(), trip.getBoardingNotes()));
    }
    private void local(LocalServiceRun run, DriverNotificationType type, String action) {
        String route = routeLabel(run.getRoute());
        create(run.getDriver(), type, "Local service " + action,
                "Your local service for " + route + " was " + action + ".",
                "LOCAL_SERVICE_RUN", run.getId(), key(action, run.getId(), id(run.getDriver()),
                        id(run.getBus()), id(run.getRoute()), run.getServiceDate(),
                        run.getPlannedStartTime(), run.getPlannedEndTime(), run.getStatus(), run.getNotes()));
    }

    private void create(DriverProfile driver, DriverNotificationType type, String title,
                        String message, String entityType, Long entityId, String eventKey) {
        if (driver == null || entityId == null) return;
        if (notifications.existsByDriverAndTypeAndEventKey(driver, type, eventKey)) return;
        DriverNotification row = new DriverNotification();
        row.setDriver(driver); row.setType(type); row.setTitle(title); row.setMessage(message);
        row.setRelatedEntityType(entityType); row.setRelatedEntityId(entityId.toString());
        row.setEventKey(eventKey);
        notifications.save(row);
    }

    private DriverProfile requireApprovedDriver(String email) {
        User user = users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Authenticated user not found."));
        if (!"DRIVER".equalsIgnoreCase(user.getRole()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Driver access is required.");
        DriverProfile driver = drivers.findByUser(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Driver profile not found."));
        if (!driver.isApproved())
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Driver application is not approved.");
        return driver;
    }
    private DriverNotificationResponse response(DriverNotification row) {
        return new DriverNotificationResponse(row.getId(), row.getType().name(), row.getTitle(),
                row.getMessage(), row.getRelatedEntityType(), row.getRelatedEntityId(),
                row.getReadAt() != null, row.getReadAt(), row.getCreatedAt());
    }
    private String routeLabel(com.yatayat.backend.entity.Route route) {
        return route == null ? "your assigned route" :
                (route.getName() == null || route.getName().isBlank()
                        ? route.getOrigin() + " to " + route.getDestination() : route.getName());
    }
    private Long id(DriverProfile value) { return value == null ? null : value.getId(); }
    private Long id(Bus value) { return value == null ? null : value.getId(); }
    private Long id(com.yatayat.backend.entity.Route value) { return value == null ? null : value.getId(); }
    private String key(Object... values) {
        String source = String.join("|", Arrays.stream(values)
                .map(value -> value == null ? "" : value.toString()).toList());
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(source.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }
}
