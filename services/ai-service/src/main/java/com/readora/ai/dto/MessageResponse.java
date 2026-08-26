package com.readora.ai.dto;

import java.time.Instant;

public record MessageResponse(String role, String content, Instant createdAt) {
}
