package com.readora.ai.controller;

import com.readora.ai.entity.BookReaderIndexStatus;
import com.readora.sharedcore.security.CurrentUserContext;
import com.readora.ai.service.BookContentIndexService;
import com.readora.ai.service.BookReaderChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

// The reader's AI assistant — a focused RAG Q&A scoped to one purchased book's own content, kept entirely separate from ChatController's general shopping assistant (different system prompt, no tool calling, no cross-book knowledge); see BookReaderChatService and BookContentIndexService.
@Tag(name = "Reader")
@RestController
@RequestMapping("/api/v1/ai/books/{bookId}/reader")
public class ReaderController {

    private final BookContentIndexService indexService;
    private final BookReaderChatService chatService;

    // Injects the indexing and reader-chat services.
    public ReaderController(BookContentIndexService indexService, BookReaderChatService chatService) {
        this.indexService = indexService;
        this.chatService = chatService;
    }

    // Returns whether this book's content has been embedded for the assistant yet.
    @Operation(
            summary = "Has this book's content been embedded for the assistant yet?",
            description = "Null status means indexing has never been triggered for this book — the frontend should offer to initialize it.",
            tags = {"Reader"}
    )
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Status returned")})
    @GetMapping("/status")
    public ResponseEntity<StatusResponse> status(@PathVariable UUID bookId) {
        return ResponseEntity.ok(new StatusResponse(indexService.getStatus(bookId)));
    }

    // Embeds this book's content for the assistant, a no-op if already ready.
    @Operation(
            summary = "Embed this book's content for the assistant",
            description = "One-time, shared across every owner of this book — a no-op if it's already ready. Requires the caller to have purchased this book.",
            tags = {"Reader"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Final status returned (READY or FAILED)"),
            @ApiResponse(responseCode = "403", description = "The caller hasn't purchased this book")
    })
    @PostMapping("/initialize")
    public ResponseEntity<StatusResponse> initialize(@PathVariable UUID bookId) {
        BookReaderIndexStatus status = indexService.initialize(CurrentUserContext.require(), bookId);
        return ResponseEntity.ok(new StatusResponse(status));
    }

    // Returns this reader's chat history for this book, oldest first.
    @Operation(
            summary = "This reader's chat history for this book",
            tags = {"Reader"}
    )
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Messages returned, oldest first (possibly empty)")})
    @GetMapping("/messages")
    public ResponseEntity<List<BookReaderChatService.ReaderMessage>> messages(@PathVariable UUID bookId) {
        return ResponseEntity.ok(chatService.getHistory(CurrentUserContext.require(), bookId));
    }

    // Asks the reader assistant a question, answered strictly from this book's own embedded content.
    @Operation(
            summary = "Ask the reader assistant a question about this book",
            description = "Answers strictly from the book's own embedded content. Requires the book to be indexed first — see /initialize.",
            tags = {"Reader"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reply returned"),
            @ApiResponse(responseCode = "403", description = "The caller hasn't purchased this book"),
            @ApiResponse(responseCode = "409", description = "The book hasn't been indexed yet")
    })
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@PathVariable UUID bookId, @RequestBody ChatRequest request) {
        String reply = chatService.chat(CurrentUserContext.require(), bookId, request.message());
        return ResponseEntity.ok(new ChatResponse(reply));
    }

    // Response body carrying this book's indexing status.
    public record StatusResponse(BookReaderIndexStatus status) {
    }

    // Request body for a reader chat question.
    public record ChatRequest(@NotBlank String message) {
    }

    // Response body carrying the assistant's reply.
    public record ChatResponse(String reply) {
    }
}
