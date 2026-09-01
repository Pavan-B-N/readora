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
class UserScopedToolCallbackTest {

    @Mock private ToolCallback delegate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void call_overwritesModelSuppliedUserIdWithTheRealOne() throws Exception {
        UserScopedToolCallback callback = new UserScopedToolCallback(delegate, "real-user-id", objectMapper);
        when(delegate.call(org.mockito.ArgumentMatchers.anyString())).thenReturn("{}");

        callback.call("{\"userId\":\"model-invented-user-id\"}");

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(delegate).call(captor.capture());
        ObjectNode sent = (ObjectNode) objectMapper.readTree(captor.getValue());
        assertThat(sent.get("userId").asText()).isEqualTo("real-user-id");
    }

    @Test
    void call_toolInputWithNoUserIdField_addsIt() throws Exception {
        UserScopedToolCallback callback = new UserScopedToolCallback(delegate, "real-user-id", objectMapper);
        when(delegate.call(org.mockito.ArgumentMatchers.anyString())).thenReturn("{}");

        callback.call("{}");

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(delegate).call(captor.capture());
        ObjectNode sent = (ObjectNode) objectMapper.readTree(captor.getValue());
        assertThat(sent.get("userId").asText()).isEqualTo("real-user-id");
    }

    @Test
    void call_malformedToolInput_throwsIllegalState() {
        UserScopedToolCallback callback = new UserScopedToolCallback(delegate, "real-user-id", objectMapper);
        var definition = org.mockito.Mockito.mock(org.springframework.ai.tool.definition.ToolDefinition.class);
        when(definition.name()).thenReturn("getCart");
        when(delegate.getToolDefinition()).thenReturn(definition);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> callback.call("not json"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void getToolDefinition_delegatesToWrappedCallback() {
        UserScopedToolCallback callback = new UserScopedToolCallback(delegate, "u1", objectMapper);
        var definition = org.mockito.Mockito.mock(org.springframework.ai.tool.definition.ToolDefinition.class);
        when(delegate.getToolDefinition()).thenReturn(definition);

        assertThat(callback.getToolDefinition()).isSameAs(definition);
    }
}
