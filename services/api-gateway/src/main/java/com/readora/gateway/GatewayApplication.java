package com.readora.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

// Entry point for the reactive API gateway service.
@SpringBootApplication
@ConfigurationPropertiesScan
public class GatewayApplication {
    // Boots the Spring application context.
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
