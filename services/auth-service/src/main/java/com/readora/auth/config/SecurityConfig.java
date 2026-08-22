package com.readora.auth.config;

import com.readora.auth.filter.CorrelationIdFilter;
import com.readora.auth.filter.GatewaySecretFilter;
import com.readora.auth.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/** Spring Security wiring: stateless sessions, public auth endpoints, and the custom filter chain order. */
@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CorrelationIdFilter correlationIdFilter;
    private final GatewaySecretFilter gatewaySecretFilter;

    /**
     * @param jwtAuthenticationFilter validates Bearer tokens and populates the security context
     * @param correlationIdFilter     assigns/propagates the request's correlation id
     * @param gatewaySecretFilter     rejects requests that didn't come through api-gateway
     */
    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            CorrelationIdFilter correlationIdFilter,
            GatewaySecretFilter gatewaySecretFilter
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.correlationIdFilter = correlationIdFilter;
        this.gatewaySecretFilter = gatewaySecretFilter;
    }

    /** @return the BCrypt password encoder used to hash and verify user passwords */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Builds the security filter chain: stateless sessions, CSRF disabled (this is a stateless
     * JSON API, not browser-form-based), register/login/refresh public, everything else
     * requires authentication, with GatewaySecretFilter running first, then CorrelationIdFilter,
     * then JwtAuthenticationFilter.
     *
     * @param http the HttpSecurity builder to configure
     * @return the configured SecurityFilterChain
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/v1/auth/register",
                                "/api/v1/auth/login",
                                "/api/v1/auth/refresh"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(correlationIdFilter, JwtAuthenticationFilter.class)
                .addFilterBefore(gatewaySecretFilter, CorrelationIdFilter.class);

        return http.build();
    }
}
