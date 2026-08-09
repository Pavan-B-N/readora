package com.readora.mcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

// Entry point for the MCP server, which exposes Readora's tools to AI agents.
@SpringBootApplication(scanBasePackages = {"com.readora.mcp", "com.readora.sharedcore"})
@ConfigurationPropertiesScan(basePackages = {"com.readora.mcp", "com.readora.sharedcore"})
public class McpServerApplication {
    // Boots the Spring application context.
    public static void main(String[] args) {
        SpringApplication.run(McpServerApplication.class, args);
    }
}
