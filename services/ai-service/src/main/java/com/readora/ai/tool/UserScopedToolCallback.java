package com.readora.ai.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

// Wraps an MCP tool callback that declares userId as a parameter and forces it to the caller's real, JWT-authenticated user id before the call goes out, overwriting whatever value the model itself supplied — without this the model would choose whose data a tool like getCart or getOrderHistory reads, exactly the gap flagged in ChatService's own history.
public class UserScopedToolCallback implements ToolCallback {

    private final ToolCallback delegate;
    private final String userId;
    private final ObjectMapper objectMapper;

    // Wraps delegate so every call is rewritten with the caller's real user id.
    public UserScopedToolCallback(ToolCallback delegate, String userId, ObjectMapper objectMapper) {
        this.delegate = delegate;
        this.userId = userId;
        this.objectMapper = objectMapper;
    }

    // Delegates to the wrapped tool's own definition.
    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    // Rewrites userId in the tool input, then delegates the call.
    @Override
    public String call(String toolInput) {
        return delegate.call(rewriteUserId(toolInput));
    }

    // Overwrites the userId field of the JSON tool input with the caller's real user id.
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
