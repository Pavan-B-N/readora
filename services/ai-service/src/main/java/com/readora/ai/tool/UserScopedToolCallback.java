package com.readora.ai.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * Wraps an MCP tool callback that declares userId as a parameter, and forces that parameter to
 * the caller's real, JWT-authenticated user id before the call goes out — overwriting whatever
 * value the model itself supplied. Without this, the model chooses whose data a tool like getCart
 * or getOrderHistory reads, which is exactly the gap flagged in ChatService's own history.
 */
public class UserScopedToolCallback implements ToolCallback {

    private final ToolCallback delegate;
    private final String userId;
    private final ObjectMapper objectMapper;

    public UserScopedToolCallback(ToolCallback delegate, String userId, ObjectMapper objectMapper) {
        this.delegate = delegate;
        this.userId = userId;
        this.objectMapper = objectMapper;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public String call(String toolInput) {
        return delegate.call(rewriteUserId(toolInput));
    }

    private String rewriteUserId(String toolInput) {
        try {
            ObjectNode node = (ObjectNode) objectMapper.readTree(toolInput);
            node.put("userId", userId);
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to rewrite userId for tool " + delegate.getToolDefinition().name(), e
            );
        }
    }
}
