package com.readora.notification.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

// Configures the STOMP-over-WebSocket message broker: endpoint, destinations, and inbound auth.
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;
    private final WebSocketProperties webSocketProperties;

    // Wires in the STOMP auth interceptor and the configured allowed-origins list.
    public WebSocketConfig(StompAuthChannelInterceptor stompAuthChannelInterceptor, WebSocketProperties webSocketProperties) {
        this.stompAuthChannelInterceptor = stompAuthChannelInterceptor;
        this.webSocketProperties = webSocketProperties;
    }

    // Registers the /ws SockJS endpoint with the configured allowed origins.
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(webSocketProperties.allowedOrigins().toArray(String[]::new))
                .withSockJS();
    }

    // Enables the simple in-memory broker and sets destination prefixes.
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    // Registers the STOMP auth interceptor on the inbound client channel.
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthChannelInterceptor);
    }
}
