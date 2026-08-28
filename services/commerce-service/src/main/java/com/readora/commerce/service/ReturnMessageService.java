package com.readora.commerce.service;

import com.readora.commerce.dto.ReturnMessageResponse;
import com.readora.commerce.entity.Order;
import com.readora.commerce.entity.OrderStatus;
import com.readora.commerce.entity.ReturnMessage;
import com.readora.commerce.entity.ReturnSenderRole;
import com.readora.commerce.exception.ReturnNotUnderReviewException;
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

    public ReturnMessageService(ReturnMessageRepository repository) {
        this.repository = repository;
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
        return toResponse(message);
    }

    private ReturnMessageResponse toResponse(ReturnMessage message) {
        return new ReturnMessageResponse(
                message.getId(), message.getSenderUserId(), message.getSenderRole().name(),
                message.getContent(), message.getCreatedAt()
        );
    }
}
