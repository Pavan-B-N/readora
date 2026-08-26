package com.readora.notification.service;

import com.readora.notification.dto.NotificationPayload;
import com.readora.notification.dto.NotificationResponse;
import com.readora.notification.entity.Notification;
import com.readora.notification.exception.NotificationNotFoundException;
import com.readora.notification.repository.NotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class NotificationService {

    private static final String DESTINATION = "/queue/notifications";

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationService(NotificationRepository notificationRepository, SimpMessagingTemplate messagingTemplate) {
        this.notificationRepository = notificationRepository;
        this.messagingTemplate = messagingTemplate;
    }

    /** Persists the notification, then pushes it live to whoever's connected — a page can load history even if it wasn't. */
    @Transactional
    public void create(UUID userId, String type, String title, String message, UUID orderId) {
        Notification notification = notificationRepository.save(new Notification(userId, type, title, message, orderId));
        messagingTemplate.convertAndSendToUser(
                userId.toString(), DESTINATION, new NotificationPayload(type, toResponse(notification))
        );
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> list(UUID userId, Pageable pageable) {
        return notificationRepository.findAllByUserIdOrderByCreatedAtDesc(userId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public long unreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public void markRead(UUID userId, UUID notificationId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(NotificationNotFoundException::new);
        notification.markRead();
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllRead(UUID userId) {
        notificationRepository.markAllRead(userId);
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(), notification.getType(), notification.getTitle(), notification.getMessage(),
                notification.getOrderId(), notification.isRead(), notification.getCreatedAt()
        );
    }
}
