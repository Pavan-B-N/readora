package com.readora.commerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

// Spring Boot entry point for the commerce service.
@SpringBootApplication(scanBasePackages = {"com.readora.commerce", "com.readora.sharedcore"})
@ConfigurationPropertiesScan(basePackages = {"com.readora.commerce", "com.readora.sharedcore"})
@EnableScheduling
public class CommerceServiceApplication {
    // Boots the Spring application context.
    public static void main(String[] args) {
        SpringApplication.run(CommerceServiceApplication.class, args);
    }
}
