package com.readora.notification.config;

import com.readora.sharedcore.security.JwtService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Validates the caller's JWT on STOMP CONNECT and binds their user id as the session principal —
 * connections without a valid Bearer token are rejected outright. Defence-in-depth: the gateway
 * already validates the JWT at the HTTP Upgrade request, but this check runs regardless of how
 * the connection reached this service.
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;

    public StompAuthChannelInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            UUID userId = extractUserId(accessor.getFirstNativeHeader("Authorization"))
                    .orElseThrow(() -> new MessagingException("Missing or invalid Authorization token"));
            accessor.setUser(new StompPrincipal(userId.toString()));
        }

        return message;
    }

    private Optional<UUID> extractUserId(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Optional.empty();
        }
        return jwtService.extractUserId(authHeader.substring(7));
    }
}
