package com.readora.ai.controller;

import com.readora.ai.dto.ConversationSummaryResponse;
import com.readora.ai.security.CurrentUserContext;
import com.readora.ai.service.ConversationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI Conversations")
@RestController
@RequestMapping("/api/v1/ai/conversations")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @Operation(
            summary = "List saved conversations",
            description = "Lists the caller's saved assistant conversations, newest first.",
            tags = {"AI Conversations"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Conversation list returned")
    })
    @GetMapping
    public ResponseEntity<Page<ConversationSummaryResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(conversationService.list(CurrentUserContext.require(), pageable));
    }
}
