package com.readora.gateway.config;

import com.readora.sharedcore.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Wires up the one bean this reactive (WebFlux) service wants from the common module — JwtService, a plain POJO with no servlet dependency; deliberately not a widened @ComponentScan into com.readora.sharedcore, since that package also holds servlet-based @Component filters built on OncePerRequestFilter that have no meaning in a reactive context with no servlet container.
@Configuration
public class CommonBeansConfig {

    // Builds the shared JwtService from the configured signing secret.
    @Bean
    public JwtService jwtService(@Value("${app.jwt.secret}") String secret) {
        return new JwtService(secret);
    }
}
