package com.readora.ai.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

// Wraps a book-retrieval tool callback that declares storeId as a parameter and forces it to the caller's real, request-supplied store id before the call goes out (same mechanism as UserScopedToolCallback, applied to a different field), since without this the model's own possibly absent, wrong, or invented storeId would reach ReadoraInternalTools.filterAvailable — exactly the guardrail this exists to prevent: a book recommendation that isn't actually purchasable at the caller's store.
public class StoreScopedToolCallback implements ToolCallback {

    private final ToolCallback delegate;
    private final String storeId;
    private final ObjectMapper objectMapper;

    // Wraps delegate so every call is rewritten with the caller's real store id.
    public StoreScopedToolCallback(ToolCallback delegate, String storeId, ObjectMapper objectMapper) {
        this.delegate = delegate;
        this.storeId = storeId;
        this.objectMapper = objectMapper;
    }

    // Delegates to the wrapped tool's own definition.
    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    // Rewrites storeId in the tool input, then delegates the call.
    @Override
    public String call(String toolInput) {
        return delegate.call(rewriteStoreId(toolInput));
    }

    // Overwrites (or nulls) the storeId field of the JSON tool input with the caller's real store id.
    private String rewriteStoreId(String toolInput) {
        try {
            ObjectNode node = (ObjectNode) objectMapper.readTree(toolInput);
            if (storeId != null) {
                node.put("storeId", storeId);
            } else {
                node.putNull("storeId");
            }
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to rewrite storeId for tool " + delegate.getToolDefinition().name(), e
            );
        }
    }
}
