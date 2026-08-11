package com.readora.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

// Spring Boot entry point for ai-service, scanning both its own package and shared-core.
@SpringBootApplication(scanBasePackages = {"com.readora.ai", "com.readora.sharedcore"})
@ConfigurationPropertiesScan(basePackages = {"com.readora.ai", "com.readora.sharedcore"})
public class AiServiceApplication {
    // Boots the Spring application context.
    public static void main(String[] args) {
        SpringApplication.run(AiServiceApplication.class, args);
    }
}
