package com.readora.ai.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.readora.sharedcore.event.BookUpsertedEvent;
import com.readora.ai.service.EmbeddingService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class BookEventsListener {

    private final EmbeddingService embeddingService;
    private final ObjectMapper objectMapper;

    public BookEventsListener(EmbeddingService embeddingService, ObjectMapper objectMapper) {
        this.embeddingService = embeddingService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = KafkaTopics.BOOK_UPSERTED, groupId = "ai-service")
    public void onBookUpserted(String payload) throws Exception {
        BookUpsertedEvent event = objectMapper.readValue(payload, BookUpsertedEvent.class);
        embeddingService.embedOne(event.bookId());
    }
}
