package com.readora.ai.config;

import com.readora.ai.tool.ReadoraInternalTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires internal tools (in-process) and the MCP client's tools (mcp-server, over streamable
 * HTTP — the ToolCallbackProvider bean here is auto-configured by spring-ai-starter-mcp-client
 * from spring.ai.mcp.client.* properties) into one ChatClient. Unverified against a live build —
 * see build notes, and the userId-injection gap noted in ChatService.
 */
@Configuration
public class ChatClientConfig {

    @Bean
    public ChatClient chatClient(
            ChatClient.Builder builder,
            ReadoraInternalTools internalTools,
            ToolCallbackProvider mcpToolCallbackProvider
    ) {
        return builder
                .defaultTools(internalTools)
                .defaultToolCallbacks(mcpToolCallbackProvider.getToolCallbacks())
                .build();
    }
}
