package com.readora.notification.repository;

import com.readora.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

// Spring Data JPA repository for a user's notifications.
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    // Fetches a user's notifications, newest first, paginated.
    Page<Notification> findAllByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    // Fetches one notification, scoped to its owning user.
    Optional<Notification> findByIdAndUserId(UUID id, UUID userId);

    // Counts a user's unread notifications.
    long countByUserIdAndReadFalse(UUID userId);

    // Bulk-marks all of a user's unread notifications as read.
    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.userId = :userId AND n.read = false")
    void markAllRead(@Param("userId") UUID userId);
}
