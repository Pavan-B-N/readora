package com.readora.ai.dto;

import java.time.Instant;
import java.util.List;

// One turn of a conversation, with any book ids the assistant referenced in it.
public record MessageResponse(String role, String content, Instant createdAt, List<String> bookIds) {
}
