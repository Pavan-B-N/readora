package com.readora.gateway.security;

import java.util.UUID;

public record AuthenticatedPrincipal(UUID userId, String email) {
}
