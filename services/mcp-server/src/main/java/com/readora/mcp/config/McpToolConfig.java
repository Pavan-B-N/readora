package com.readora.mcp.config;

import com.readora.mcp.tool.ReadoraMcpTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Registers ReadoraMcpTools' @Tool methods as MCP tools — the WebMVC MCP server auto-configuration picks up ToolCallbackProvider beans and exposes them over the streamable HTTP transport automatically; unverified against a live build, see build notes.
@Configuration
public class McpToolConfig {

    // Wraps ReadoraMcpTools' @Tool methods as a ToolCallbackProvider bean.
    @Bean
    public ToolCallbackProvider readoraToolCallbackProvider(ReadoraMcpTools tools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(tools)
                .build();
    }
}
