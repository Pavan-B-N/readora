package com.readora.notification.service;

import com.readora.notification.entity.Notification;
import com.readora.notification.exception.NotificationNotFoundException;
import com.readora.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private NotificationService notificationService;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(notificationRepository, messagingTemplate);
    }

    @Test
    void create_persistsThenPushesToTheUsersQueue() {
        UUID orderId = UUID.randomUUID();
        Notification saved = new Notification(userId, "ORDER_DELIVERED", "Delivered", "Your order arrived", orderId);
        when(notificationRepository.save(any(Notification.class))).thenReturn(saved);

        notificationService.create(userId, "ORDER_DELIVERED", "Delivered", "Your order arrived", orderId);

        verify(notificationRepository).save(any(Notification.class));
        verify(messagingTemplate).convertAndSendToUser(eq(userId.toString()), eq("/queue/notifications"), any());
    }

    @Test
    void markRead_wrongUser_throwsNotFoundRatherThanLeakingItExists() {
        UUID notificationId = UUID.randomUUID();
        when(notificationRepository.findByIdAndUserId(notificationId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markRead(userId, notificationId))
                .isInstanceOf(NotificationNotFoundException.class);
    }

    @Test
    void markRead_ownNotification_marksItReadAndSaves() {
        UUID orderId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        Notification notification = new Notification(userId, "ORDER_DELIVERED", "Delivered", "Your order arrived", orderId);
        when(notificationRepository.findByIdAndUserId(notificationId, userId)).thenReturn(Optional.of(notification));

        notificationService.markRead(userId, notificationId);

        assertThat(notification.isRead()).isTrue();
        verify(notificationRepository).save(notification);
    }

    @Test
    void markAllRead_delegatesDirectlyToTheBulkRepositoryUpdate() {
        notificationService.markAllRead(userId);

        verify(notificationRepository).markAllRead(userId);
    }

    @Test
    void unreadCount_delegatesToRepositoryCount() {
        when(notificationRepository.countByUserIdAndReadFalse(userId)).thenReturn(Long.valueOf(3L));

        long count = notificationService.unreadCount(userId);

        assertThat(count).isEqualTo(3L);
    }
}
