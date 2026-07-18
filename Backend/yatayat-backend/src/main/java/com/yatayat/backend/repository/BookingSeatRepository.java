package com.yatayat.backend.repository;

import com.yatayat.backend.entity.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface BookingSeatRepository extends JpaRepository<BookingSeat, Long> {
    List<BookingSeat> findByScheduledTripOrderBySeatNumberAsc(ScheduledTrip trip);
    List<BookingSeat> findByScheduledTripAndPassengerAndStatusOrderBySeatNumberAsc(
            ScheduledTrip trip, User passenger, BookingSeatStatus status);
    List<BookingSeat> findByBookingOrderBySeatNumberAsc(PassengerTripBooking booking);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<BookingSeat> findWithLockByBookingOrderBySeatNumberAsc(PassengerTripBooking booking);

    @Modifying(flushAutomatically = true)
    @Query("""
            update BookingSeat seat set seat.status = com.yatayat.backend.entity.BookingSeatStatus.RELEASED,
                seat.activeSeatNumber = null
            where seat.scheduledTrip = :trip
              and seat.status = com.yatayat.backend.entity.BookingSeatStatus.HELD
              and seat.holdExpiresAt <= :now
            """)
    int releaseExpired(@Param("trip") ScheduledTrip trip, @Param("now") LocalDateTime now);
}
