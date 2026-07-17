package com.yatayat.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "booking_seats",
        uniqueConstraints = @UniqueConstraint(name = "uk_booking_seat_active_trip",
                columnNames = {"scheduled_trip_id", "active_seat_number"}),
        indexes = {
                @Index(name = "idx_booking_seat_trip_status", columnList = "scheduled_trip_id,status"),
                @Index(name = "idx_booking_seat_passenger_expiry", columnList = "passenger_id,hold_expires_at")
        })
public class BookingSeat {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "booking_id",
            foreignKey = @ForeignKey(name = "fk_booking_seat_booking"))
    private PassengerTripBooking booking;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "scheduled_trip_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_booking_seat_trip"))
    private ScheduledTrip scheduledTrip;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "passenger_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_booking_seat_passenger"))
    private User passenger;
    @Column(name = "seat_number", nullable = false, length = 12) private String seatNumber;
    @Column(name = "active_seat_number", length = 12) private String activeSeatNumber;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private BookingSeatStatus status;
    @Column(name = "held_at", nullable = false) private LocalDateTime heldAt;
    @Column(name = "hold_expires_at", nullable = false) private LocalDateTime holdExpiresAt;
    @Column(nullable = false) private LocalDateTime createdAt;
    @Column(nullable = false) private LocalDateTime updatedAt;

    @PrePersist void create() { LocalDateTime now = LocalDateTime.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void update() { updatedAt = LocalDateTime.now(); }
    public Long getId() { return id; }
    public PassengerTripBooking getBooking() { return booking; }
    public ScheduledTrip getScheduledTrip() { return scheduledTrip; }
    public User getPassenger() { return passenger; }
    public String getSeatNumber() { return seatNumber; }
    public String getActiveSeatNumber() { return activeSeatNumber; }
    public BookingSeatStatus getStatus() { return status; }
    public LocalDateTime getHeldAt() { return heldAt; }
    public LocalDateTime getHoldExpiresAt() { return holdExpiresAt; }
    public void setBooking(PassengerTripBooking value) { booking = value; }
    public void setScheduledTrip(ScheduledTrip value) { scheduledTrip = value; }
    public void setPassenger(User value) { passenger = value; }
    public void setSeatNumber(String value) { seatNumber = normalize(value); }
    public void setActiveSeatNumber(String value) { activeSeatNumber = normalize(value); }
    public void setStatus(BookingSeatStatus value) { status = value; }
    public void setHeldAt(LocalDateTime value) { heldAt = value; }
    public void setHoldExpiresAt(LocalDateTime value) { holdExpiresAt = value; }
    public void release(BookingSeatStatus releasedStatus) { status = releasedStatus; activeSeatNumber = null; }
    private String normalize(String value) { return value == null ? null : value.trim().toUpperCase(); }
}
