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

/**
 * All-read-only MCP tool surface (see the doc's Security section — no addToCart/placeOrder/
 * cancelOrder tool exists anywhere in this system; mutations only happen via the authenticated
 * REST API driven by the user's own click). Every user-scoped tool takes userId explicitly since
 * ai-service passes through the caller's authenticated identity — the model can never name a
 * different user's id and read their data, because ai-service controls what it's allowed to pass.
 */
@Component
public class ReadoraMcpTools {

    private final CatalogClient catalogClient;
    private final CommerceClient commerceClient;
    private final UserClient userClient;

    public ReadoraMcpTools(CatalogClient catalogClient, CommerceClient commerceClient, UserClient userClient) {
        this.catalogClient = catalogClient;
        this.commerceClient = commerceClient;
        this.userClient = userClient;
    }

    @Tool(description = "Search the book catalogue by free-text query and return matching books with price and availability")
    public List<BookSummary> searchBooks(@ToolParam(description = "free-text search query") String query) {
        return catalogClient.search(query).items();
    }

    @Tool(description = "Get the full book record for one book id, including format and publisher")
    public BookDetail getBookDetails(@ToolParam(description = "the book's id") String bookId) {
        return catalogClient.getDetail(bookId);
    }

    @Tool(description = "Check live stock availability and quantity for one book")
    public BookDetail.Availability checkInventory(@ToolParam(description = "the book's id") String bookId) {
        return catalogClient.getDetail(bookId).availability();
    }

    @Tool(description = "Get the caller's order history, newest first")
    public List<OrderSummary> getOrderHistory(@ToolParam(description = "the caller's user id") String userId) {
        return commerceClient.getOrderHistory(userId).items();
    }

    @Tool(description = "Get the caller's current cart contents and subtotal")
    public CartInfo getCart(@ToolParam(description = "the caller's user id") String userId) {
        return commerceClient.getCart(userId);
    }

    @Tool(description = "Get the caller's display name, locale and profile info")
    public ProfileInfo getUserProfile(@ToolParam(description = "the caller's user id") String userId) {
        return userClient.getProfile(userId);
    }

    @Tool(description = "Get the caller's current wallet balance")
    public WalletInfo getWalletBalance(@ToolParam(description = "the caller's user id") String userId) {
        return userClient.getWallet(userId);
    }
}
