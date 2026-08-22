package com.readora.notification.service;

import com.readora.notification.dto.NotificationPayload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class NotificationService {

    private static final String DESTINATION = "/queue/notifications";

    private final SimpMessagingTemplate messagingTemplate;

    public NotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void push(UUID userId, String type, Object data) {
        messagingTemplate.convertAndSendToUser(userId.toString(), DESTINATION, new NotificationPayload(type, data));
    }
}
