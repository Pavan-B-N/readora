package com.readora.ai.config;

import com.readora.ai.tool.ReadoraInternalTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.support.ToolCallbacks;
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

    /**
     * Two jobs: (1) get the model writing real Markdown, since the frontend renders it as such
     * (lists, bold, links) rather than a flat text blob; (2) get an accurate, self-reported list of
     * which book ids a given reply actually recommends, via a leading hidden marker that
     * ChatService.stripLeadingReferenceMarker peels off before anything reaches the client. That
     * replaces an earlier approach of re-running a fresh vector search against the user's message
     * after the fact — which routinely surfaced books the reply never actually mentioned, because
     * "semantically near the question" and "what the model chose to recommend" are different
     * things. Asking the model to self-report is the only way to know which candidates it settled
     * on; tool results below carry a real id specifically so it has ids worth citing.
     */
    private static final String SYSTEM_PROMPT = """
            You are Readora's book-shopping assistant. Format every reply in Markdown: use "- " \
            for bullet lists, blank lines between paragraphs, and **bold** for emphasis.

            Before your reply, on the very first line and with nothing else on that line, output a \
            hidden reference marker in exactly this format:
            <!--REFERENCE_BOOKS:["id1","id2"]-->
            CRITICAL: the array must contain exactly one id for every book you actually name in your \
            visible reply below — no more, no fewer — in the same order you name them. A tool call \
            may return several candidates; do not list ids for ones you looked at but didn't end up \
            writing about. If your reply names 3 books, the array has exactly 3 ids. Use an empty \
            array — <!--REFERENCE_BOOKS:[]--> — if your reply doesn't name any specific book at all. \
            Only use ids that appeared in a tool result (ragSearchBooks, recommendSimilarBooks, or \
            compareBooks); never invent one. Then start your visible reply on the next line — never \
            mention or explain the marker itself.

            Every time you name a specific book in your reply, link its title inline using \
            [Book Title](/books/{id}), where {id} is that same book's real id from a tool result. \
            Always use a relative path starting with "/books/" — never a full URL with a scheme or \
            hostname.

            ragSearchBooks, recommendSimilarBooks, and compareBooks already filter their results to \
            books actually purchasable right now (in stock at the caller's store, or virtual) — every \
            id they return is safe to recommend. Ignore their storeId parameter entirely; it's filled \
            in automatically and any value you supply is discarded. If a tool call returns fewer \
            results than you expected, that's real unavailability, not an error — don't recommend a \
            book you only recall from earlier in this conversation without it appearing in a fresh \
            tool result first.
            """;

    @Bean
    public ChatClient chatClient(
            ChatClient.Builder builder,
            ToolCallbackProvider mcpToolCallbackProvider
    ) {
        List<ToolCallback> safeMcpCallbacks = Arrays.stream(mcpToolCallbackProvider.getToolCallbacks())
                .filter(callback -> !USER_SCOPED_TOOL_NAMES.contains(callback.getToolDefinition().name()))
                .toList();

        // ReadoraInternalTools is deliberately NOT registered as a default here — every one of its
        // book-retrieval tools takes a storeId parameter that must come from the real request, not
        // the model, so it's wired per-request instead (see internalToolCallbacksTemplate() below
        // and ChatService, which wraps each in a StoreScopedToolCallback the same way
        // userScopedToolCallbacks() below is wrapped for userId).
        return builder
                .defaultSystem(SYSTEM_PROMPT)
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

    /**
     * ReadoraInternalTools' @Tool-annotated methods, converted to plain ToolCallbacks so
     * ChatService can wrap each in a StoreScopedToolCallback per request — see the chatClient()
     * bean above for why these aren't registered as shared defaults.
     *
     * Declared as an array rather than List<ToolCallback>: Spring's own ToolCallingAutoConfiguration
     * autowires a single List<ToolCallback> bean for its internal tool resolver, and having two
     * beans of that exact type (this one and userScopedToolCallbacks below) made that injection
     * ambiguous and broke application startup. An array is a different declared type, so it no
     * longer competes for that slot.
     */
    @Bean
    public ToolCallback[] internalToolCallbacksTemplate(ReadoraInternalTools internalTools) {
        return ToolCallbacks.from(internalTools);
    }
}
