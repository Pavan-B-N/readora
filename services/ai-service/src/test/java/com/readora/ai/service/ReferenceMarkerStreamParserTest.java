package com.readora.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReferenceMarkerStreamParserTest {

    private final ReferenceMarkerStreamParser parser = new ReferenceMarkerStreamParser(new ObjectMapper());

    private List<String> capturedBookIds;

    private String parse(String... tokens) {
        capturedBookIds = null;
        Flux<String> result = parser.stripLeadingReferenceMarker(Flux.just(tokens), ids -> capturedBookIds = ids);
        return String.join("", result.collectList().block());
    }

    @Test
    void shortReplyWithoutMarker_stillFlushesReplyTextOnCompletion() {
        String joined = parse("Sure, ", "here's a recommendation.");

        assertThat(joined).isEqualTo("Sure, here's a recommendation.");
        assertThat(capturedBookIds).isEmpty();
    }

    @Test
    void markerConsumedWithOnlyTrailingWhitespaceLeft_emitsNoEmptyFrame() {
        String joined = parse("<!--REFERENCE_BOOKS:[]-->", "\n");

        assertThat(joined).isEmpty();
        assertThat(capturedBookIds).isEmpty();
    }

    @Test
    void chunksArrivingAfterResolution_passStraightThroughUnbuffered() {
        String joined = parse("<!--REFERENCE_BOOKS:[]-->\n", "Hello", " world");

        assertThat(joined).isEqualTo("Hello world");
    }

    @Test
    void replyLongEnoughToHitTheBuffer_flushesOnceThresholdReached() {
        String longChunk = "x".repeat(450);

        String joined = parse(longChunk);

        assertThat(joined).isEqualTo(longChunk);
        assertThat(capturedBookIds).isEmpty();
    }

    @Test
    void withReferenceMarker_extractsBookIds() {
        String bookId = "11111111-1111-1111-1111-111111111111";
        String marker = "<!--REFERENCE_BOOKS:[\"" + bookId + "\"]-->\n";

        String joined = parse(marker + "I recommend Clean Code.");

        assertThat(joined).isEqualTo("I recommend Clean Code.");
        assertThat(capturedBookIds).containsExactly(bookId);
    }

    @Test
    void malformedReferenceBooksJson_dropsTheMarkerRatherThanFailing() {
        String joined = parse("<!--REFERENCE_BOOKS:[not valid json]-->\n", "Sure, here you go.");

        assertThat(joined).isEqualTo("Sure, here you go.");
        assertThat(capturedBookIds).isEmpty();
    }

    @Test
    void bookIdsSinkInvokedExactlyOnce() {
        List<List<String>> invocations = new ArrayList<>();
        Flux<String> result = parser.stripLeadingReferenceMarker(
                Flux.just("<!--REFERENCE_BOOKS:[]-->\n", "Hello", " world"), invocations::add);

        result.collectList().block();

        assertThat(invocations).hasSize(1);
    }
}
