package com.yatayat.backend.repository;

import com.yatayat.backend.entity.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.*;

public interface DriverNotificationRepository extends JpaRepository<DriverNotification, Long> {
    List<DriverNotification> findByDriverOrderByCreatedAtDesc(DriverProfile driver);
    List<DriverNotification> findByDriverAndReadAtIsNullOrderByCreatedAtDesc(DriverProfile driver);
    long countByDriverAndReadAtIsNull(DriverProfile driver);
    Optional<DriverNotification> findByIdAndDriver(Long id, DriverProfile driver);
    boolean existsByDriverAndTypeAndEventKey(DriverProfile driver, DriverNotificationType type, String eventKey);
    @Modifying
    @Query("update DriverNotification n set n.readAt = :readAt where n.driver = :driver and n.readAt is null")
    int markAllRead(@Param("driver") DriverProfile driver, @Param("readAt") LocalDateTime readAt);
}
