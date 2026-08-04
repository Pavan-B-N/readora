package com.readora.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

// Entry point for the notification service.
@SpringBootApplication(scanBasePackages = {"com.readora.notification", "com.readora.sharedcore"})
@ConfigurationPropertiesScan(basePackages = {"com.readora.notification", "com.readora.sharedcore"})
public class NotificationServiceApplication {
    // Boots the Spring application context.
    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
