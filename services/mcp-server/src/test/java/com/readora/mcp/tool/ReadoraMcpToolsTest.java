package com.readora.mcp.tool;

import com.readora.mcp.client.CatalogClient;
import com.readora.mcp.client.CommerceClient;
import com.readora.mcp.client.UserClient;
import com.readora.mcp.dto.BookDetail;
import com.readora.mcp.dto.BookPage;
import com.readora.mcp.dto.BookSummary;
import com.readora.mcp.dto.CartInfo;
import com.readora.mcp.dto.OrderPage;
import com.readora.mcp.dto.ProfileInfo;
import com.readora.mcp.dto.WalletInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReadoraMcpToolsTest {

    @Mock private CatalogClient catalogClient;
    @Mock private CommerceClient commerceClient;
    @Mock private UserClient userClient;

    private ReadoraMcpTools tools;

    @BeforeEach
    void setUp() {
        tools = new ReadoraMcpTools(catalogClient, commerceClient, userClient);
    }

    @Test
    void searchBooks_unwrapsItemsFromThePage() {
        BookSummary book = new BookSummary("b1", "Clean Code", List.of("Robert Martin"), new BigDecimal("499.00"), "INR", "IN_STOCK");
        when(catalogClient.search("clean code")).thenReturn(new BookPage(List.of(book)));

        List<BookSummary> result = tools.searchBooks("clean code");

        assertThat(result).containsExactly(book);
    }

    @Test
    void getBookDetails_delegatesToClient() {
        BookDetail detail = new BookDetail("b1", "Clean Code", "Prentice Hall", new BigDecimal("499.00"), "INR",
                new BookDetail.Availability("IN_STOCK", 5));
        when(catalogClient.getDetail("b1")).thenReturn(detail);

        assertThat(tools.getBookDetails("b1")).isEqualTo(detail);
    }

    @Test
    void checkInventory_extractsAvailabilityFromBookDetail() {
        BookDetail detail = new BookDetail("b1", "Clean Code", "Prentice Hall", new BigDecimal("499.00"), "INR",
                new BookDetail.Availability("IN_STOCK", 5));
        when(catalogClient.getDetail("b1")).thenReturn(detail);

        var availability = tools.checkInventory("b1");

        assertThat(availability.status()).isEqualTo("IN_STOCK");
        assertThat(availability.quantityAvailable()).isEqualTo(5);
    }

    @Test
    void getOrderHistory_unwrapsItemsFromThePage() {
        when(commerceClient.getOrderHistory("u1")).thenReturn(new OrderPage(List.of()));

        assertThat(tools.getOrderHistory("u1")).isEmpty();
    }

    @Test
    void getCart_delegatesToCommerceClient() {
        CartInfo cart = new CartInfo(List.of(), BigDecimal.ZERO, "INR", 0);
        when(commerceClient.getCart("u1")).thenReturn(cart);

        assertThat(tools.getCart("u1")).isEqualTo(cart);
    }

    @Test
    void getUserProfile_delegatesToUserClient() {
        ProfileInfo profile = new ProfileInfo("u1", "reader@example.com", "Reader", "en-IN");
        when(userClient.getProfile("u1")).thenReturn(profile);

        assertThat(tools.getUserProfile("u1")).isEqualTo(profile);
    }

    @Test
    void getWalletBalance_delegatesToUserClient() {
        WalletInfo wallet = new WalletInfo(new BigDecimal("500.00"), "INR");
        when(userClient.getWallet("u1")).thenReturn(wallet);

        assertThat(tools.getWalletBalance("u1")).isEqualTo(wallet);
    }
}
