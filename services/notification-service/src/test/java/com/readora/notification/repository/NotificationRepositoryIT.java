package com.readora.notification.repository;

import com.readora.notification.entity.Notification;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * markAllRead is a hand-written @Modifying @Query (JPQL) — a syntax or field-name mistake in it
 * would never surface in a Mockito-backed unit test (the mock just returns whatever's stubbed),
 * only against a real database actually executing the query.
 */
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class NotificationRepositoryIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine");

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void markAllRead_flipsEveryUnreadNotificationForThatUserOnly() {
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();

        notificationRepository.save(new Notification(userId, "ORDER_PLACED", "Order placed", "Your order was placed", null));
        notificationRepository.save(new Notification(userId, "ORDER_DELIVERED", "Delivered", "Your order arrived", null));
        Notification othersNotification = notificationRepository.save(
                new Notification(otherUserId, "ORDER_PLACED", "Order placed", "Your order was placed", null)
        );
        entityManager.flush();

        notificationRepository.markAllRead(userId);
        entityManager.flush();
        entityManager.clear();

        assertThat(notificationRepository.countByUserIdAndReadFalse(userId)).isZero();
        assertThat(notificationRepository.findById(othersNotification.getId())).isPresent().get()
                .extracting(Notification::isRead).isEqualTo(false);
    }
}
