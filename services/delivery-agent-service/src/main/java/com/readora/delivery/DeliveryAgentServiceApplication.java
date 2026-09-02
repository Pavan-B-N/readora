package com.readora.delivery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(scanBasePackages = {"com.readora.delivery", "com.readora.sharedcore"})
@ConfigurationPropertiesScan(basePackages = {"com.readora.delivery", "com.readora.sharedcore"})
public class DeliveryAgentServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(DeliveryAgentServiceApplication.class, args);
    }
}
