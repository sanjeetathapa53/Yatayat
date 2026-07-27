package com.yatayat.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String routeName;
    private String busNumber;
    private String seatNumber;
    private String travelDate;
    private String departureTime;

    private Double fare;

    private String paymentStatus; // PAID, PENDING
    private String bookingStatus; // CONFIRMED, CANCELLED, COMPLETED

    private String qrCode;

    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User passenger;

    public Booking() {
    }

    public Booking(User passenger, String routeName, String busNumber,
                   String seatNumber, String travelDate, String departureTime,
                   Double fare, String qrCode) {
        this.passenger = passenger;
        this.routeName = routeName;
        this.busNumber = busNumber;
        this.seatNumber = seatNumber;
        this.travelDate = travelDate;
        this.departureTime = departureTime;
        this.fare = fare;
        this.paymentStatus = "PAID";
        this.bookingStatus = "CONFIRMED";
        this.qrCode = qrCode;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getRouteName() { return routeName; }
    public String getBusNumber() { return busNumber; }
    public String getSeatNumber() { return seatNumber; }
    public String getTravelDate() { return travelDate; }
    public String getDepartureTime() { return departureTime; }
    public Double getFare() { return fare; }
    public String getPaymentStatus() { return paymentStatus; }
    public String getBookingStatus() { return bookingStatus; }
    public String getQrCode() { return qrCode; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public User getPassenger() { return passenger; }

    public void setId(Long id) { this.id = id; }
    public void setRouteName(String routeName) { this.routeName = routeName; }
    public void setBusNumber(String busNumber) { this.busNumber = busNumber; }
    public void setSeatNumber(String seatNumber) { this.seatNumber = seatNumber; }
    public void setTravelDate(String travelDate) { this.travelDate = travelDate; }
    public void setDepartureTime(String departureTime) { this.departureTime = departureTime; }
    public void setFare(Double fare) { this.fare = fare; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    public void setBookingStatus(String bookingStatus) { this.bookingStatus = bookingStatus; }
    public void setQrCode(String qrCode) { this.qrCode = qrCode; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setPassenger(User passenger) { this.passenger = passenger; }
}
