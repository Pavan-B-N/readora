package com.readora.commerce.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.readora.commerce.client.UserServiceClient;
import com.readora.commerce.dto.NotificationRequestedEvent;
import com.readora.commerce.dto.ReturnMessageResponse;
import com.readora.commerce.entity.Order;
import com.readora.commerce.entity.OrderStatus;
import com.readora.commerce.entity.OutboxEvent;
import com.readora.commerce.entity.ReturnMessage;
import com.readora.commerce.entity.ReturnSenderRole;
import com.readora.commerce.exception.ReturnNotUnderReviewException;
import com.readora.commerce.kafka.KafkaTopics;
import com.readora.commerce.repository.OutboxEventRepository;
import com.readora.commerce.repository.ReturnMessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * The small chat that opens on a return while it's RETURN_REQUESTED — shared by both
 * OrderService (customer side) and AdminOrderService (admin side), each of which does its own
 * ownership/store check before calling in here with an already-loaded, already-authorized Order.
 */
@Service
public class ReturnMessageService {

    private final ReturnMessageRepository repository;
    private final UserServiceClient userServiceClient;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public ReturnMessageService(
            ReturnMessageRepository repository,
            UserServiceClient userServiceClient,
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper
    ) {
        this.repository = repository;
        this.userServiceClient = userServiceClient;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<ReturnMessageResponse> list(UUID orderId) {
        return repository.findAllByOrderIdOrderByCreatedAt(orderId).stream().map(this::toResponse).toList();
    }

    /** Locked once a decision is made — nothing left to discuss once the return is approved/rejected. */
    @Transactional
    public ReturnMessageResponse post(Order order, UUID senderUserId, ReturnSenderRole role, String content) {
        if (order.getStatus() != OrderStatus.RETURN_REQUESTED) {
            throw new ReturnNotUnderReviewException();
        }
        ReturnMessage message = repository.save(new ReturnMessage(order, senderUserId, role, content));
        notifyOtherParty(order, role);
        return toResponse(message);
    }

    /** Whoever didn't just send this message — the customer if an admin wrote it, or that store's admin if the customer did. */
    private void notifyOtherParty(Order order, ReturnSenderRole senderRole) {
        UUID recipientUserId = senderRole == ReturnSenderRole.ADMIN
                ? order.getUserId()
                : userServiceClient.getAdminUserIdForStore(order.getStoreId());
        if (recipientUserId == null) {
            return;
        }

        String title = senderRole == ReturnSenderRole.ADMIN ? "New message from support" : "New message on a return";
        String message = "You have a new message on order " + order.getOrderNumber() + "'s return.";

        try {
            String json = objectMapper.writeValueAsString(new NotificationRequestedEvent(
                    recipientUserId, "RETURN_MESSAGE", title, message, order.getId()
            ));
            outboxEventRepository.save(new OutboxEvent("Order", order.getId(), KafkaTopics.NOTIFICATION_REQUESTED, json));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize outbox event payload", e);
        }
    }

    private ReturnMessageResponse toResponse(ReturnMessage message) {
        return new ReturnMessageResponse(
                message.getId(), message.getSenderUserId(), message.getSenderRole().name(),
                message.getContent(), message.getCreatedAt()
        );
    }
}
