package com.yatayat.backend.repository;

import com.yatayat.backend.entity.Booking;
import com.yatayat.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByPassengerOrderByCreatedAtDesc(User passenger);
    Optional<Booking> findByQrCode(String qrCode);


    boolean existsByBusNumberAndTravelDateAndSeatNumberAndBookingStatusNot(
            String busNumber,
            String travelDate,
            String seatNumber,
            String bookingStatus
    );
}