package com.readora.commerce.service;

import com.readora.commerce.entity.OutboxEvent;
import com.readora.commerce.repository.OutboxEventRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

// Polls the outbox table and publishes unpublished events to Kafka, giving at-least-once delivery from the same transaction as the business write.
@Component
public class OutboxRelay {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    // Wires the outbox repository and Kafka template.
    public OutboxRelay(OutboxEventRepository outboxEventRepository, KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    // Publishes every unpublished outbox event, run on a fixed 2s delay.
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
