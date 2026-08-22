package com.readora.user.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.readora.user.dto.PaymentCapturedEvent;
import com.readora.user.dto.RefundCompletedEvent;
import com.readora.user.service.WalletEventService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class WalletEventsListener {

    private final WalletEventService walletEventService;
    private final ObjectMapper objectMapper;

    public WalletEventsListener(WalletEventService walletEventService, ObjectMapper objectMapper) {
        this.walletEventService = walletEventService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = KafkaTopics.PAYMENT_CAPTURED, groupId = "user-service")
    public void onPaymentCaptured(String payload) throws Exception {
        walletEventService.handlePaymentCaptured(objectMapper.readValue(payload, PaymentCapturedEvent.class));
    }

    @KafkaListener(topics = KafkaTopics.REFUND_COMPLETED, groupId = "user-service")
    public void onRefundCompleted(String payload) throws Exception {
        walletEventService.handleRefundCompleted(objectMapper.readValue(payload, RefundCompletedEvent.class));
    }
}
