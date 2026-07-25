package com.readora.catalog.service;

import com.readora.catalog.entity.OutboxEvent;
import com.readora.catalog.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxRelayTest {

    @Mock private OutboxEventRepository outboxEventRepository;
    @Mock private KafkaTemplate<String, String> kafkaTemplate;

    private OutboxRelay outboxRelay;

    private static OutboxEvent event() throws Exception {
        OutboxEvent event = new OutboxEvent("Book", UUID.randomUUID(), "book.upserted", "{}");
        Field idField = OutboxEvent.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(event, UUID.randomUUID());
        return event;
    }

    @Test
    void relay_noUnpublishedEvents_isANoOp() {
        outboxRelay = new OutboxRelay(outboxEventRepository, kafkaTemplate);
        when(outboxEventRepository.findAllByPublishedAtIsNull()).thenReturn(List.of());

        outboxRelay.relay();

        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    void relay_unpublishedEvents_sendsEachAndMarksPublished() throws Exception {
        outboxRelay = new OutboxRelay(outboxEventRepository, kafkaTemplate);
        OutboxEvent event = event();
        when(outboxEventRepository.findAllByPublishedAtIsNull()).thenReturn(List.of(event));

        outboxRelay.relay();

        verify(kafkaTemplate, times(1)).send("book.upserted", event.getId().toString(), "{}");
        verify(outboxEventRepository).save(event);

        Field publishedAtField = OutboxEvent.class.getDeclaredField("publishedAt");
        publishedAtField.setAccessible(true);
        assertThat(publishedAtField.get(event)).isNotNull();
    }
}
