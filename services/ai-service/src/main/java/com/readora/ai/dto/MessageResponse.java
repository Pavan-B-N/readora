package com.readora.ai.dto;

import java.time.Instant;
import java.util.List;

public record MessageResponse(String role, String content, Instant createdAt, List<String> bookIds) {
}
