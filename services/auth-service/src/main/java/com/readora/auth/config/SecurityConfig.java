package com.readora.auth.config;

import com.readora.sharedcore.filter.CorrelationIdFilter;
import com.readora.sharedcore.filter.GatewaySecretFilter;
import com.readora.sharedcore.config.SecurityProperties;
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
    private final SecurityProperties securityProperties;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            CorrelationIdFilter correlationIdFilter,
            GatewaySecretFilter gatewaySecretFilter,
            SecurityProperties securityProperties
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.correlationIdFilter = correlationIdFilter;
        this.gatewaySecretFilter = gatewaySecretFilter;
        this.securityProperties = securityProperties;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * CSRF disabled — this is a stateless JSON API, not browser-form-based. Filter order:
     * GatewaySecretFilter, then CorrelationIdFilter, then JwtAuthenticationFilter.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        String[] publicRoutes = securityProperties.publicRoutes().toArray(String[]::new);

        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // kubelet's probes hit this directly, never carrying a JWT — same exemption
                        // shared-core's own filters apply for every other service (see UserContextFilter).
                        .requestMatchers("/actuator/health/**").permitAll()
                        .requestMatchers(publicRoutes).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(correlationIdFilter, JwtAuthenticationFilter.class)
                .addFilterBefore(gatewaySecretFilter, CorrelationIdFilter.class);

        return http.build();
    }
}
