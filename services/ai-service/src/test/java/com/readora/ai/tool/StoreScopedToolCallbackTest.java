package com.readora.ai.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.tool.ToolCallback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoreScopedToolCallbackTest {

    @Mock private ToolCallback delegate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void call_overwritesModelSuppliedStoreIdWithTheRealOne() throws Exception {
        StoreScopedToolCallback callback = new StoreScopedToolCallback(delegate, "real-store-id", objectMapper);
        when(delegate.call(org.mockito.ArgumentMatchers.anyString())).thenReturn("[]");

        callback.call("{\"query\":\"clean code\",\"storeId\":\"model-invented-store-id\"}");

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(delegate).call(captor.capture());
        ObjectNode sent = (ObjectNode) objectMapper.readTree(captor.getValue());
        assertThat(sent.get("storeId").asText()).isEqualTo("real-store-id");
        assertThat(sent.get("query").asText()).isEqualTo("clean code");
    }

    @Test
    void call_nullStoreId_setsExplicitJsonNull() throws Exception {
        StoreScopedToolCallback callback = new StoreScopedToolCallback(delegate, null, objectMapper);
        when(delegate.call(org.mockito.ArgumentMatchers.anyString())).thenReturn("[]");

        callback.call("{\"query\":\"clean code\"}");

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(delegate).call(captor.capture());
        ObjectNode sent = (ObjectNode) objectMapper.readTree(captor.getValue());
        assertThat(sent.get("storeId").isNull()).isTrue();
    }

    @Test
    void call_malformedToolInput_throwsIllegalState() {
        StoreScopedToolCallback callback = new StoreScopedToolCallback(delegate, "real-store-id", objectMapper);
        var definition = org.mockito.Mockito.mock(org.springframework.ai.tool.definition.ToolDefinition.class);
        when(definition.name()).thenReturn("ragSearchBooks");
        when(delegate.getToolDefinition()).thenReturn(definition);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> callback.call("not json"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void getToolDefinition_delegatesToWrappedCallback() {
        StoreScopedToolCallback callback = new StoreScopedToolCallback(delegate, "s1", objectMapper);
        var definition = org.mockito.Mockito.mock(org.springframework.ai.tool.definition.ToolDefinition.class);
        when(delegate.getToolDefinition()).thenReturn(definition);

        assertThat(callback.getToolDefinition()).isSameAs(definition);
    }
}
