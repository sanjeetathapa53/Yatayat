package com.yatayat.backend.repository;

import com.yatayat.backend.entity.Notification;
import com.yatayat.backend.entity.NotificationType;
import com.yatayat.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByRecipientOrderByCreatedAtDesc(User recipient);
    long countByRecipientAndReadAtIsNull(User recipient);
    Optional<Notification> findByIdAndRecipient(Long id, User recipient);
    boolean existsByRecipientAndTypeAndReferenceId(
            User recipient, NotificationType type, String referenceId);

    @Modifying
    @Query("""
            update Notification notification
               set notification.readAt = :readAt
             where notification.recipient = :recipient
               and notification.readAt is null
            """)
    int markAllRead(@Param("recipient") User recipient,
                    @Param("readAt") LocalDateTime readAt);
}
