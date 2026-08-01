package com.readora.payment.service;

import com.readora.payment.entity.OutboxEvent;
import com.readora.payment.repository.OutboxEventRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/** Polls unpublished outbox rows and hands them to Kafka. Simple @Scheduled poll rather than a CDC/Debezium-based relay — sufficient at this scale, and keeps the moving parts down to what's actually running locally (no separate CDC connector to operate). */
@Component
public class OutboxRelay {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    // Wires the repository and Kafka template this relay publishes through.
    public OutboxRelay(OutboxEventRepository outboxEventRepository, KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    // Publishes every unpublished outbox event to Kafka and marks it published.
    @Scheduled(fixedDelay = 2000)
    public void relay() {
        List<OutboxEvent> unpublished = outboxEventRepository.findAllByPublishedAtIsNull();

        for (OutboxEvent event : unpublished) {
            kafkaTemplate.send(event.getEventType(), event.getId().toString(), event.getPayload());
            event.markPublished();
            outboxEventRepository.save(event);
        }
    }
}
