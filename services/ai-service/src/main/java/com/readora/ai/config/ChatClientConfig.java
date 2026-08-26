package com.readora.ai.config;

import com.readora.ai.tool.ReadoraInternalTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Wires internal tools (in-process) and the MCP client's tools (mcp-server, over streamable
 * HTTP — the ToolCallbackProvider bean here is auto-configured by spring-ai-starter-mcp-client
 * from spring.ai.mcp.client.* properties) into one ChatClient. Unverified against a live build —
 * see build notes.
 */
@Configuration
public class ChatClientConfig {

    /**
     * mcp-server tool names that read a specific user's own data and declare userId as a
     * parameter. Deliberately excluded from the shared ChatClient's default tools below — see
     * userScopedToolCallbacks() for why.
     */
    private static final Set<String> USER_SCOPED_TOOL_NAMES =
            Set.of("getCart", "getOrderHistory", "getUserProfile", "getWalletBalance");

    @Bean
    public ChatClient chatClient(
            ChatClient.Builder builder,
            ReadoraInternalTools internalTools,
            ToolCallbackProvider mcpToolCallbackProvider
    ) {
        List<ToolCallback> safeMcpCallbacks = Arrays.stream(mcpToolCallbackProvider.getToolCallbacks())
                .filter(callback -> !USER_SCOPED_TOOL_NAMES.contains(callback.getToolDefinition().name()))
                .toList();

        return builder
                .defaultTools(internalTools)
                .defaultToolCallbacks(safeMcpCallbacks)
                .build();
    }

    /**
     * The four MCP tools that read one specific user's own data. Not registered as defaults on
     * the shared ChatClient bean above, because each takes userId as a model-visible parameter —
     * registering them directly would let the model choose whose cart, orders, profile, or wallet
     * to read. ChatService wraps each of these per-request in a UserScopedToolCallback, which
     * overwrites userId with the caller's real, JWT-authenticated id before the call goes out.
     */
    @Bean
    public List<ToolCallback> userScopedToolCallbacks(ToolCallbackProvider mcpToolCallbackProvider) {
        return Arrays.stream(mcpToolCallbackProvider.getToolCallbacks())
                .filter(callback -> USER_SCOPED_TOOL_NAMES.contains(callback.getToolDefinition().name()))
                .toList();
    }
}
