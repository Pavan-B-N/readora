package com.readora.commerce.service;

import com.readora.commerce.cart.CartItemData;
import com.readora.commerce.cart.CartRepository;
import com.readora.commerce.client.CatalogClient;
import com.readora.commerce.dto.AddCartItemRequest;
import com.readora.commerce.dto.BookInfo;
import com.readora.commerce.dto.CartResponse;
import com.readora.commerce.dto.CartSummaryResponse;
import com.readora.commerce.entity.DeliveryType;
import com.readora.commerce.exception.BookNotAvailableAtStoreException;
import com.readora.commerce.exception.CartItemNotFoundException;
import com.readora.commerce.exception.InsufficientStockException;
import com.readora.commerce.exception.QtyLimitExceededException;
import com.readora.commerce.exception.StoreIdRequiredException;
import com.readora.commerce.exception.VirtualEditionAlreadyOwnedException;
import com.readora.commerce.exception.VirtualEditionNotAvailableException;
import com.readora.commerce.repository.OrderItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;
    @Mock
    private CatalogClient catalogClient;
    @Mock
    private OrderItemRepository orderItemRepository;

    private CartService cartService;

    private final UUID userId = UUID.randomUUID();
    private final UUID bookId = UUID.randomUUID();
    private final UUID storeId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        cartService = new CartService(cartRepository, catalogClient, orderItemRepository);
    }

    private BookInfo physicalBook(int qtyAvailable) {
        return new BookInfo(bookId, "Some Title", "9781234567897", new BigDecimal("299.00"), "INR",
                new BookInfo.Availability("IN_STOCK", qtyAvailable), null);
    }

    private BookInfo virtualBook() {
        return new BookInfo(bookId, "Some Title", "9781234567897", new BigDecimal("299.00"), "INR",
                new BookInfo.Availability("NO_PHYSICAL_EDITION", 0), new BookInfo.VirtualEditionInfo(new BigDecimal("199.00"), "INR"));
    }

    @Test
    void addItem_physicalWithoutStoreId_throwsStoreIdRequired() {
        when(cartRepository.getItems(userId)).thenReturn(new ArrayList<>());

        assertThatThrownBy(() -> cartService.addItem(userId, new AddCartItemRequest(bookId, 1, DeliveryType.PHYSICAL, null)))
                .isInstanceOf(StoreIdRequiredException.class);

        verify(catalogClient, never()).getBook(any(), any());
    }

    @Test
    void addItem_physicalExceedingMaxQtyPerTitle_throws() {
        List<CartItemData> existing = new ArrayList<>(List.of(
                new CartItemData(bookId, "Some Title", 9, new BigDecimal("299.00"), DeliveryType.PHYSICAL, Instant.now())
        ));
        when(cartRepository.getItems(userId)).thenReturn(existing);

        assertThatThrownBy(() -> cartService.addItem(userId, new AddCartItemRequest(bookId, 2, DeliveryType.PHYSICAL, storeId)))
                .isInstanceOf(QtyLimitExceededException.class);
    }

    @Test
    void addItem_virtualAlreadyOwned_throwsBeforeCheckingCatalog() {
        when(cartRepository.getItems(userId)).thenReturn(new ArrayList<>());
        when(orderItemRepository.existsActiveVirtualPurchase(userId, bookId)).thenReturn(Boolean.valueOf(true));

        assertThatThrownBy(() -> cartService.addItem(userId, new AddCartItemRequest(bookId, 1, DeliveryType.VIRTUAL, null)))
                .isInstanceOf(VirtualEditionAlreadyOwnedException.class);

        verify(catalogClient, never()).getBook(any(), any());
    }

    @Test
    void addItem_virtualQty_isAlwaysClampedToOneRegardlessOfRequestedQty() {
        when(cartRepository.getItems(userId)).thenReturn(new ArrayList<>());
        when(orderItemRepository.existsActiveVirtualPurchase(userId, bookId)).thenReturn(Boolean.valueOf(false));
        when(catalogClient.getBook(bookId, null)).thenReturn(virtualBook());

        CartSummaryResponse response = cartService.addItem(userId, new AddCartItemRequest(bookId, 7, DeliveryType.VIRTUAL, null));

        assertThat(response.itemCount()).isEqualTo(1);
        assertThat(response.subtotal()).isEqualByComparingTo("199.00");
    }

    @Test
    void addItem_virtualEditionNotAvailable_throws() {
        when(cartRepository.getItems(userId)).thenReturn(new ArrayList<>());
        when(orderItemRepository.existsActiveVirtualPurchase(userId, bookId)).thenReturn(Boolean.valueOf(false));
        when(catalogClient.getBook(bookId, null)).thenReturn(physicalBook(5));

        assertThatThrownBy(() -> cartService.addItem(userId, new AddCartItemRequest(bookId, 1, DeliveryType.VIRTUAL, null)))
                .isInstanceOf(VirtualEditionNotAvailableException.class);
    }

    @Test
    void addItem_physicalWithinLimits_accumulatesQtyAndSavesCart() {
        List<CartItemData> existing = new ArrayList<>(List.of(
                new CartItemData(bookId, "Some Title", 2, new BigDecimal("299.00"), DeliveryType.PHYSICAL, Instant.now())
        ));
        when(cartRepository.getItems(userId)).thenReturn(existing);
        when(catalogClient.getBook(bookId, storeId)).thenReturn(physicalBook(20));

        CartSummaryResponse response = cartService.addItem(userId, new AddCartItemRequest(bookId, 3, DeliveryType.PHYSICAL, storeId));

        assertThat(response.itemCount()).isEqualTo(5);
        assertThat(response.subtotal()).isEqualByComparingTo(new BigDecimal("299.00").multiply(BigDecimal.valueOf(5)));
        verify(cartRepository).saveItems(any(), any());
    }

    @Test
    void addItem_wrongStore_throwsBookNotAvailableAtStore() {
        when(cartRepository.getItems(userId)).thenReturn(new ArrayList<>());
        BookInfo book = new BookInfo(bookId, "Some Title", "9781234567897", new BigDecimal("299.00"), "INR",
                new BookInfo.Availability("NOT_AVAILABLE_AT_STORE", 0), null);
        when(catalogClient.getBook(bookId, storeId)).thenReturn(book);

        assertThatThrownBy(() -> cartService.addItem(userId, new AddCartItemRequest(bookId, 1, DeliveryType.PHYSICAL, storeId)))
                .isInstanceOf(BookNotAvailableAtStoreException.class);
    }

    @Test
    void addItem_insufficientStock_throws() {
        when(cartRepository.getItems(userId)).thenReturn(new ArrayList<>());
        when(catalogClient.getBook(bookId, storeId)).thenReturn(physicalBook(1));

        assertThatThrownBy(() -> cartService.addItem(userId, new AddCartItemRequest(bookId, 5, DeliveryType.PHYSICAL, storeId)))
                .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    void getCart_mapsItemsAndFlagsShippingRequiredForPhysical() {
        List<CartItemData> items = List.of(
                new CartItemData(bookId, "Some Title", 2, new BigDecimal("299.00"), DeliveryType.PHYSICAL, Instant.now())
        );
        when(cartRepository.getItems(userId)).thenReturn(items);

        CartResponse response = cartService.getCart(userId);

        assertThat(response.requiresShippingAddress()).isTrue();
        assertThat(response.itemCount()).isEqualTo(2);
    }

    @Test
    void setItemQty_itemNotInCart_throws() {
        when(cartRepository.getItems(userId)).thenReturn(new ArrayList<>());

        assertThatThrownBy(() -> cartService.setItemQty(userId, bookId, DeliveryType.PHYSICAL, 3))
                .isInstanceOf(CartItemNotFoundException.class);
    }

    @Test
    void setItemQty_zero_removesItem() {
        List<CartItemData> items = new ArrayList<>(List.of(
                new CartItemData(bookId, "Some Title", 2, new BigDecimal("299.00"), DeliveryType.PHYSICAL, Instant.now())
        ));
        when(cartRepository.getItems(userId)).thenReturn(items);

        CartSummaryResponse response = cartService.setItemQty(userId, bookId, DeliveryType.PHYSICAL, 0);

        assertThat(response.itemCount()).isZero();
        verify(catalogClient, never()).getBook(any(), any());
    }

    @Test
    void setItemQty_exceedsLimit_throws() {
        List<CartItemData> items = new ArrayList<>(List.of(
                new CartItemData(bookId, "Some Title", 2, new BigDecimal("299.00"), DeliveryType.PHYSICAL, Instant.now())
        ));
        when(cartRepository.getItems(userId)).thenReturn(items);

        assertThatThrownBy(() -> cartService.setItemQty(userId, bookId, DeliveryType.PHYSICAL, 11))
                .isInstanceOf(QtyLimitExceededException.class);
    }

    @Test
    void setItemQty_virtual_clampsToOneRegardlessOfRequestedQty() {
        List<CartItemData> items = new ArrayList<>(List.of(
                new CartItemData(bookId, "Some Title", 1, new BigDecimal("199.00"), DeliveryType.VIRTUAL, Instant.now())
        ));
        when(cartRepository.getItems(userId)).thenReturn(items);
        when(catalogClient.getBook(bookId, null)).thenReturn(virtualBook());

        CartSummaryResponse response = cartService.setItemQty(userId, bookId, DeliveryType.VIRTUAL, 5);

        assertThat(response.itemCount()).isEqualTo(1);
    }

    @Test
    void setItemQty_valid_updatesQtyAndSaves() {
        List<CartItemData> items = new ArrayList<>(List.of(
                new CartItemData(bookId, "Some Title", 2, new BigDecimal("299.00"), DeliveryType.PHYSICAL, Instant.now())
        ));
        when(cartRepository.getItems(userId)).thenReturn(items);
        when(catalogClient.getBook(bookId, null)).thenReturn(physicalBook(20));

        CartSummaryResponse response = cartService.setItemQty(userId, bookId, DeliveryType.PHYSICAL, 4);

        assertThat(response.itemCount()).isEqualTo(4);
        verify(cartRepository).saveItems(any(), any());
    }
}
