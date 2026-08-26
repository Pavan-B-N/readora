package com.readora.delivery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class DeliveryAgentServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(DeliveryAgentServiceApplication.class, args);
    }
}
