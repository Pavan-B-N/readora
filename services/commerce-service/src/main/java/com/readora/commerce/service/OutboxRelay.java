package com.readora.commerce.service;

import com.readora.commerce.entity.OutboxEvent;
import com.readora.commerce.repository.OutboxEventRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OutboxRelay {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxRelay(OutboxEventRepository outboxEventRepository, KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

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
