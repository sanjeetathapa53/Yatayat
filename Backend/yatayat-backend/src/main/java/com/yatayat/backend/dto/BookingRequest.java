package com.yatayat.backend.dto;

public class BookingRequest {

    private Long userId;
    private String routeName;
    private String busNumber;
    private String seatNumber;
    private String travelDate;
    private String departureTime;
    private Double fare;

    // NEW
    private String walletPin;

    public BookingRequest() {
    }

    public Long getUserId() {
        return userId;
    }

    public String getRouteName() {
        return routeName;
    }

    public String getBusNumber() {
        return busNumber;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public String getTravelDate() {
        return travelDate;
    }

    public String getDepartureTime() {
        return departureTime;
    }

    public Double getFare() {
        return fare;
    }

    public String getWalletPin() {
        return walletPin;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setRouteName(String routeName) {
        this.routeName = routeName;
    }

    public void setBusNumber(String busNumber) {
        this.busNumber = busNumber;
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }

    public void setTravelDate(String travelDate) {
        this.travelDate = travelDate;
    }

    public void setDepartureTime(String departureTime) {
        this.departureTime = departureTime;
    }

    public void setFare(Double fare) {
        this.fare = fare;
    }

    public void setWalletPin(String walletPin) {
        this.walletPin = walletPin;
    }
}