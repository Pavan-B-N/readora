package com.readora.gateway.security;

import java.util.List;
import java.util.UUID;

public record AuthenticatedPrincipal(UUID userId, String email, List<String> roles) {
}
