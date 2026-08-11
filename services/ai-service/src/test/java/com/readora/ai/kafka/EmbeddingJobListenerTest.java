package com.readora.ai.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.readora.ai.dto.EmbeddingBackfillRequestedEvent;
import com.readora.ai.service.EmbeddingJobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmbeddingJobListenerTest {

    @Mock private EmbeddingJobService embeddingJobService;

    private EmbeddingJobListener listener;

    @BeforeEach
    void setUp() {
        listener = new EmbeddingJobListener(embeddingJobService, new ObjectMapper());
    }

    @Test
    void onBackfillRequested_runsTheJobNamedInThePayload() throws Exception {
        UUID jobId = UUID.randomUUID();
        String payload = new ObjectMapper().writeValueAsString(new EmbeddingBackfillRequestedEvent(jobId));

        listener.onBackfillRequested(payload);

        verify(embeddingJobService).runBackfill(jobId);
    }
}
