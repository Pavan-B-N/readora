package com.readora.notification.config;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

/**
 * Simplified handshake auth: trusts a "userId" STOMP CONNECT header rather than validating the
 * caller's JWT — the notification WebSocket endpoint isn't routed through api-gateway in this
 * pass (proxying WebSocket/STOMP through Spring Cloud Gateway is real added complexity deferred
 * for later). A production version would validate the JWT here, same as every other service.
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String userId = accessor.getFirstNativeHeader("userId");
            if (userId != null && !userId.isBlank()) {
                accessor.setUser(new StompPrincipal(userId));
            }
        }

        return message;
    }
}
