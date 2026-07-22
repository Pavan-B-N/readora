package com.readora.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(scanBasePackages = {"com.readora.user", "com.readora.sharedcore"})
@ConfigurationPropertiesScan(basePackages = {"com.readora.user", "com.readora.sharedcore"})
// Spring Boot entry point for the user service.
public class UserServiceApplication {
    // Boots the Spring application context.
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
