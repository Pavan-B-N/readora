package com.readora.mcp.tool;

import com.readora.mcp.client.CatalogClient;
import com.readora.mcp.client.CommerceClient;
import com.readora.mcp.client.UserClient;
import com.readora.mcp.dto.BookDetail;
import com.readora.mcp.dto.BookSummary;
import com.readora.mcp.dto.CartInfo;
import com.readora.mcp.dto.OrderSummary;
import com.readora.mcp.dto.ProfileInfo;
import com.readora.mcp.dto.WalletInfo;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

// All-read-only MCP tool surface (see the doc's Security section — no addToCart/placeOrder/cancelOrder tool exists anywhere in this system; mutations only happen via the authenticated REST API driven by the user's own click); every user-scoped tool takes userId explicitly since ai-service passes through the caller's authenticated identity, so the model can never name a different user's id and read their data.
@Component
public class ReadoraMcpTools {

    private final CatalogClient catalogClient;
    private final CommerceClient commerceClient;
    private final UserClient userClient;

    // Wires in the downstream clients backing each tool.
    public ReadoraMcpTools(CatalogClient catalogClient, CommerceClient commerceClient, UserClient userClient) {
        this.catalogClient = catalogClient;
        this.commerceClient = commerceClient;
        this.userClient = userClient;
    }

    // MCP tool: searches the catalog by free-text query.
    @Tool(description = "Search the book catalogue by free-text query and return matching books with price and availability")
    public List<BookSummary> searchBooks(@ToolParam(description = "free-text search query") String query) {
        return catalogClient.search(query).items();
    }

    // MCP tool: fetches full details for one book.
    @Tool(description = "Get the full book record for one book id, including publisher")
    public BookDetail getBookDetails(@ToolParam(description = "the book's id") String bookId) {
        return catalogClient.getDetail(bookId);
    }

    // MCP tool: checks live stock availability for one book.
    @Tool(description = "Check live stock availability and quantity for one book")
    public BookDetail.Availability checkInventory(@ToolParam(description = "the book's id") String bookId) {
        return catalogClient.getDetail(bookId).availability();
    }

    // MCP tool: fetches the caller's order history.
    @Tool(description = "Get the caller's order history, newest first")
    public List<OrderSummary> getOrderHistory(@ToolParam(description = "the caller's user id") String userId) {
        return commerceClient.getOrderHistory(userId).items();
    }

    // MCP tool: fetches the caller's current cart.
    @Tool(description = "Get the caller's current cart contents and subtotal")
    public CartInfo getCart(@ToolParam(description = "the caller's user id") String userId) {
        return commerceClient.getCart(userId);
    }

    // MCP tool: fetches the caller's profile info.
    @Tool(description = "Get the caller's display name, locale and profile info")
    public ProfileInfo getUserProfile(@ToolParam(description = "the caller's user id") String userId) {
        return userClient.getProfile(userId);
    }

    // MCP tool: fetches the caller's wallet balance.
    @Tool(description = "Get the caller's current wallet balance")
    public WalletInfo getWalletBalance(@ToolParam(description = "the caller's user id") String userId) {
        return userClient.getWallet(userId);
    }
}
