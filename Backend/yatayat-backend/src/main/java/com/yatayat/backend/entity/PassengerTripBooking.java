package com.yatayat.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "passenger_trip_bookings", uniqueConstraints =
        @UniqueConstraint(name = "uk_passenger_trip_booking_reference", columnNames = "booking_reference"),
        indexes = {
                @Index(name = "idx_ptb_passenger_booked", columnList = "passenger_id,booked_at"),
                @Index(name = "idx_ptb_trip_status", columnList = "scheduled_trip_id,status")
        })
public class PassengerTripBooking {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_reference", nullable = false, length = 32, unique = true)
    private String bookingReference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "passenger_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_ptb_passenger"))
    private User passenger;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scheduled_trip_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_ptb_scheduled_trip"))
    private ScheduledTrip scheduledTrip;

    @Column(nullable = false, length = 120) private String passengerName;
    @Column(nullable = false, length = 24) private String passengerPhone;
    @Column(nullable = false) private Integer numberOfSeats;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal farePerSeat;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal totalFare;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private BookingStatus status = BookingStatus.CONFIRMED;
    @Column(nullable = false) private LocalDateTime bookedAt;
    private LocalDateTime cancelledAt;
    @Column(nullable = false) private LocalDateTime createdAt;
    @Column(nullable = false) private LocalDateTime updatedAt;
    @Version private Long version;
    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL)
    @OrderBy("seatNumber asc")
    private List<BookingSeat> seats = new ArrayList<>();

    @PrePersist void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (status == null) status = BookingStatus.CONFIRMED;
        if (bookedAt == null) bookedAt = now;
        createdAt = now; updatedAt = now;
    }
    @PreUpdate void onUpdate() { updatedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public String getBookingReference() { return bookingReference; }
    public User getPassenger() { return passenger; }
    public ScheduledTrip getScheduledTrip() { return scheduledTrip; }
    public String getPassengerName() { return passengerName; }
    public String getPassengerPhone() { return passengerPhone; }
    public Integer getNumberOfSeats() { return numberOfSeats; }
    public BigDecimal getFarePerSeat() { return farePerSeat; }
    public BigDecimal getTotalFare() { return totalFare; }
    public BookingStatus getStatus() { return status; }
    public LocalDateTime getBookedAt() { return bookedAt; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }
    public List<BookingSeat> getSeats() { return seats; }
    public void setId(Long value) { id = value; }
    public void setBookingReference(String value) { bookingReference = value; }
    public void setPassenger(User value) { passenger = value; }
    public void setScheduledTrip(ScheduledTrip value) { scheduledTrip = value; }
    public void setPassengerName(String value) { passengerName = clean(value); }
    public void setPassengerPhone(String value) { passengerPhone = clean(value); }
    public void setNumberOfSeats(Integer value) { numberOfSeats = value; }
    public void setFarePerSeat(BigDecimal value) { farePerSeat = value; }
    public void setTotalFare(BigDecimal value) { totalFare = value; }
    public void setStatus(BookingStatus value) { status = value; }
    public void setBookedAt(LocalDateTime value) { bookedAt = value; }
    public void setCancelledAt(LocalDateTime value) { cancelledAt = value; }
    public void setCreatedAt(LocalDateTime value) { createdAt = value; }
    public void setUpdatedAt(LocalDateTime value) { updatedAt = value; }
    public void setSeats(List<BookingSeat> value) { seats = value == null ? new ArrayList<>() : value; }
    private String clean(String value) { return value == null ? null : value.trim(); }
}
