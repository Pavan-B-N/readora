package com.readora.ai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Peels the leading "<!--REFERENCE_BOOKS:[...]-->" marker ChatClientConfig's system prompt
 * instructs the model to emit as the very first thing in every reply off of the raw token
 * stream, before any of it reaches the client — split out of ChatService so the
 * buffer/regex/completion-flush plumbing is testable on its own, independent of conversation
 * persistence or the model client.
 */
@Component
public class ReferenceMarkerStreamParser {

    private static final Logger log = LoggerFactory.getLogger(ReferenceMarkerStreamParser.class);

    private static final Pattern LEADING_REFERENCE_BOOKS_PATTERN =
            Pattern.compile("^\\s*<!--REFERENCE_BOOKS:(\\[[^\\]]*])-->\\s*", Pattern.DOTALL);

    // Generous enough for the marker itself (a JSON array of a handful of UUID strings) plus a
    // little slack for however the model chunks its tokens. If nothing matching the pattern has
    // appeared by then, treat it as "the model didn't include one" rather than buffering forever
    // and never showing the reply.
    private static final int MAX_MARKER_HEADER_CHARS = 400;

    private final ObjectMapper objectMapper;

    public ReferenceMarkerStreamParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Buffers just enough of the start of the model's raw output to recognize (or rule out) the
     * leading REFERENCE_BOOKS marker, hands the parsed ids to bookIdsSink exactly once, and passes
     * everything after the marker straight through unbuffered. If the model never produces a
     * well-formed marker within MAX_MARKER_HEADER_CHARS, gives up, reports no book ids, and flushes
     * whatever was buffered as ordinary reply text — a model that ignores the instruction should
     * degrade to "no carousel", not an empty or truncated reply.
     */
    public Flux<String> stripLeadingReferenceMarker(Flux<String> tokens, Consumer<List<String>> bookIdsSink) {
        StringBuilder pending = new StringBuilder();
        AtomicBoolean markerFound = new AtomicBoolean(false);
        AtomicBoolean fullyResolved = new AtomicBoolean(false);

        Flux<String> handled = tokens.handle((chunk, sink) -> {
            if (fullyResolved.get()) {
                sink.next(chunk);
                return;
            }

            pending.append(chunk);

            if (!markerFound.get()) {
                Matcher matcher = LEADING_REFERENCE_BOOKS_PATTERN.matcher(pending);
                if (matcher.find()) {
                    markerFound.set(true);
                    bookIdsSink.accept(parseBookIdsJson(matcher.group(1)));
                    pending.delete(0, matcher.end());
                } else if (pending.length() >= MAX_MARKER_HEADER_CHARS) {
                    fullyResolved.set(true);
                    bookIdsSink.accept(List.of());
                    sink.next(pending.toString());
                    return;
                } else {
                    return; // still might be an in-progress marker — keep buffering silently.
                }
            }

            // The marker pattern's trailing "\s*" only consumes whatever whitespace had already
            // arrived by the time it matched — a chunk boundary can easily land the newline right
            // after "-->" in a separate chunk from the marker itself. Keep trimming across chunks
            // until real content shows up, rather than resolving immediately and letting a lone
            // leading newline slip through into the visible reply as if it were reply text.
            String withoutLeadingWhitespace = stripLeadingWhitespace(pending.toString());
            if (!withoutLeadingWhitespace.isEmpty()) {
                fullyResolved.set(true);
                sink.next(withoutLeadingWhitespace);
                pending.setLength(0);
            }
        });

        // Flux.handle() has no onComplete callback, so a reply that's both shorter than
        // MAX_MARKER_HEADER_CHARS and never produces the marker would otherwise leave whatever's
        // sitting in `pending` buffered forever — the client would see nothing but the trailing
        // bookIds frame. This runs once `handled` completes and flushes it, mirroring the
        // give-up-and-flush behavior above (just triggered by completion instead of by length).
        Mono<String> flushPendingOnComplete = Mono.fromCallable(() -> {
            if (fullyResolved.get()) {
                return "";
            }
            if (!markerFound.get()) {
                // Never even matched a partial marker — same "the model didn't include one"
                // verdict as the MAX_MARKER_HEADER_CHARS branch, just reached via completion.
                bookIdsSink.accept(List.of());
                return pending.toString();
            }
            // Marker matched, but whatever followed it was whitespace-only (or nothing) on every
            // chunk seen so far — trim it the same way the steady-state path does.
            return stripLeadingWhitespace(pending.toString());
        }).filter(text -> !text.isEmpty());

        return handled.concatWith(flushPendingOnComplete);
    }

    private static String stripLeadingWhitespace(String s) {
        int i = 0;
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
        return s.substring(i);
    }

    private List<String> parseBookIdsJson(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            log.warn("Model's REFERENCE_BOOKS marker wasn't valid JSON: {}", json, e);
            return List.of();
        }
    }
}
