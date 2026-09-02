package com.readora.gateway.config;

import com.readora.sharedcore.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires up the one bean this reactive (WebFlux) service actually wants from the common module —
 * JwtService, a plain POJO with no servlet dependency. Deliberately NOT a widened
 * {@code @ComponentScan} into com.readora.sharedcore: that package also holds servlet-based
 * {@code @Component} filters (JwtAuthenticationFilter, UserContextFilter, GatewaySecretFilter,
 * CorrelationIdFilter) built on {@code OncePerRequestFilter}, which have no meaning — and no safe,
 * well-defined behavior — in a reactive application context with no servlet container.
 */
@Configuration
public class CommonBeansConfig {

    @Bean
    public JwtService jwtService(@Value("${app.jwt.secret}") String secret) {
        return new JwtService(secret);
    }
}
