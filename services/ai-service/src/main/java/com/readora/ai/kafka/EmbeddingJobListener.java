package com.readora.ai.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.readora.ai.dto.EmbeddingBackfillRequestedEvent;
import com.readora.ai.service.EmbeddingJobService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class EmbeddingJobListener {

    private final EmbeddingJobService embeddingJobService;
    private final ObjectMapper objectMapper;

    public EmbeddingJobListener(EmbeddingJobService embeddingJobService, ObjectMapper objectMapper) {
        this.embeddingJobService = embeddingJobService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = KafkaTopics.EMBEDDING_BACKFILL_REQUESTED, groupId = "ai-service-backfill")
    public void onBackfillRequested(String payload) throws Exception {
        EmbeddingBackfillRequestedEvent event = objectMapper.readValue(payload, EmbeddingBackfillRequestedEvent.class);
        embeddingJobService.runBackfill(event.jobId());
    }
}
