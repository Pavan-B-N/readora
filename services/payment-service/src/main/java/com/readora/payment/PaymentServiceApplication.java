package com.readora.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

// Spring Boot entry point for the payment service.
@SpringBootApplication(scanBasePackages = {"com.readora.payment", "com.readora.sharedcore"})
@ConfigurationPropertiesScan(basePackages = {"com.readora.payment", "com.readora.sharedcore"})
@EnableScheduling
public class PaymentServiceApplication {
    // Boots the Spring application context.
    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}
