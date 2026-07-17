package com.readora.auth.config;

import com.readora.sharedcore.exception.GlobalExceptionHandler;
import com.readora.sharedcore.filter.CorrelationIdFilter;
import com.readora.sharedcore.filter.GatewaySecretFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Wires up the pieces of the common module this service actually wants, explicitly — deliberately NOT a widened @ComponentScan into com.readora.sharedcore, since that package also holds JwtAuthenticationFilter and UserContextFilter (built around the CurrentUserContext ThreadLocal this service doesn't use, authenticating via Spring Security's SecurityContextHolder instead) which Spring Boot would auto-register into the servlet chain regardless, causing UserContextFilter to 401 every request since nothing here populates CurrentUserContext.
@Configuration
@EnableConfigurationProperties(com.readora.sharedcore.config.SecurityProperties.class)
public class CommonBeansConfig {

    // Registers the shared correlation-id filter bean.
    @Bean
    public CorrelationIdFilter correlationIdFilter() {
        return new CorrelationIdFilter();
    }

    // Registers the shared gateway-secret filter bean.
    @Bean
    public GatewaySecretFilter gatewaySecretFilter(
            @Value("${app.gateway.secret}") String secret, ObjectMapper objectMapper
    ) {
        return new GatewaySecretFilter(secret, objectMapper);
    }

    // Registers the shared global exception handler bean.
    @Bean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }
}
