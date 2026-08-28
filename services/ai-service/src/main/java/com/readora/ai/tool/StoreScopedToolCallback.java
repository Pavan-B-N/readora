package com.readora.ai.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * Wraps a book-retrieval tool callback that declares storeId as a parameter, and forces that
 * parameter to the caller's real, request-supplied store id before the call goes out — same
 * mechanism as UserScopedToolCallback, applied to a different field. Without this, the model's own
 * (possibly absent, wrong, or invented) storeId value would reach ReadoraInternalTools.filterAvailable,
 * which is exactly the guardrail this exists to prevent: a book recommendation that isn't actually
 * purchasable at the caller's store.
 */
public class StoreScopedToolCallback implements ToolCallback {

    private final ToolCallback delegate;
    private final String storeId;
    private final ObjectMapper objectMapper;

    public StoreScopedToolCallback(ToolCallback delegate, String storeId, ObjectMapper objectMapper) {
        this.delegate = delegate;
        this.storeId = storeId;
        this.objectMapper = objectMapper;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public String call(String toolInput) {
        return delegate.call(rewriteStoreId(toolInput));
    }

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
