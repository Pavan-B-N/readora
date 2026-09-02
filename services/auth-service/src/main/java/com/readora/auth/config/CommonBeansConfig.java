package com.readora.auth.config;

import com.readora.sharedcore.exception.GlobalExceptionHandler;
import com.readora.sharedcore.filter.CorrelationIdFilter;
import com.readora.sharedcore.filter.GatewaySecretFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires up the pieces of the common module this service actually wants, explicitly — deliberately
 * NOT a widened {@code @ComponentScan} into com.readora.sharedcore. That package also holds
 * JwtAuthenticationFilter and UserContextFilter, built around CurrentUserContext (a ThreadLocal)
 * — this service authenticates through Spring Security's own SecurityContextHolder instead (see
 * SecurityConfig and its own, separate security.JwtAuthenticationFilter, issuer-side JwtService),
 * so those two common beans would either sit inert or actively conflict (Spring Boot
 * auto-registers any Filter bean into the servlet chain, whether or not SecurityConfig references
 * it — UserContextFilter would then 401 every request, since nothing here ever populates
 * CurrentUserContext).
 */
@Configuration
@EnableConfigurationProperties(com.readora.sharedcore.config.SecurityProperties.class)
public class CommonBeansConfig {

    @Bean
    public CorrelationIdFilter correlationIdFilter() {
        return new CorrelationIdFilter();
    }

    @Bean
    public GatewaySecretFilter gatewaySecretFilter(
            @Value("${app.gateway.secret}") String secret, ObjectMapper objectMapper
    ) {
        return new GatewaySecretFilter(secret, objectMapper);
    }

    @Bean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }
}
