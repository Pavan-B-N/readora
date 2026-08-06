package com.readora.delivery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

// Spring Boot entry point for the delivery-agent-service.
@SpringBootApplication(scanBasePackages = {"com.readora.delivery", "com.readora.sharedcore"})
@ConfigurationPropertiesScan(basePackages = {"com.readora.delivery", "com.readora.sharedcore"})
public class DeliveryAgentServiceApplication {
    // Boots the Spring application context.
    public static void main(String[] args) {
        SpringApplication.run(DeliveryAgentServiceApplication.class, args);
    }
}
