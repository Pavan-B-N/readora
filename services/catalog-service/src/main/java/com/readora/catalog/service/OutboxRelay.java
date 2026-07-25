package com.readora.catalog.service;

import com.readora.catalog.entity.OutboxEvent;
import com.readora.catalog.repository.OutboxEventRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

// Polls the outbox table and publishes any pending events to Kafka, implementing the transactional outbox pattern.
@Component
public class OutboxRelay {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    // Wires in the outbox repository and Kafka template.
    public OutboxRelay(OutboxEventRepository outboxEventRepository, KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    // Publishes every unpublished outbox event to Kafka, then marks it published.
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
