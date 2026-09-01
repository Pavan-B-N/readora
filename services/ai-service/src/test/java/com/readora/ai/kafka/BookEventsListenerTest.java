package com.readora.ai.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.readora.ai.dto.BookUpsertedEvent;
import com.readora.ai.service.EmbeddingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BookEventsListenerTest {

    @Mock private EmbeddingService embeddingService;

    private BookEventsListener listener;

    @BeforeEach
    void setUp() {
        listener = new BookEventsListener(embeddingService, new ObjectMapper());
    }

    @Test
    void onBookUpserted_embedsTheBookNamedInThePayload() throws Exception {
        UUID bookId = UUID.randomUUID();
        String payload = new ObjectMapper().writeValueAsString(new BookUpsertedEvent(bookId));

        listener.onBookUpserted(payload);

        verify(embeddingService).embedOne(bookId);
    }
}
