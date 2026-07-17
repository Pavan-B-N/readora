package com.readora.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

// Spring Boot entry point for the auth service.
@SpringBootApplication
@ConfigurationPropertiesScan
public class AuthServiceApplication {
    // Boots the Spring application context.
    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class,args);
    }
}
