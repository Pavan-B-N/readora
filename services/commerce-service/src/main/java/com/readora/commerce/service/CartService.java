package com.readora.commerce.service;

import com.readora.commerce.cart.CartItemData;
import com.readora.commerce.cart.CartRepository;
import com.readora.commerce.client.CatalogClient;
import com.readora.commerce.dto.AddCartItemRequest;
import com.readora.commerce.dto.BookInfo;
import com.readora.commerce.dto.CartResponse;
import com.readora.commerce.dto.CartSummaryResponse;
import com.readora.commerce.entity.DeliveryType;
import com.readora.commerce.exception.CartItemNotFoundException;
import com.readora.commerce.exception.InsufficientStockException;
import com.readora.commerce.exception.QtyLimitExceededException;
import com.readora.commerce.exception.VirtualEditionNotAvailableException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class CartService {

    private static final int MAX_QTY_PER_TITLE = 10;

    private final CartRepository cartRepository;
    private final CatalogClient catalogClient;

    public CartService(CartRepository cartRepository, CatalogClient catalogClient) {
        this.cartRepository = cartRepository;
        this.catalogClient = catalogClient;
    }

    public CartResponse getCart(UUID userId) {
        return toResponse(cartRepository.getItems(userId));
    }

    public CartSummaryResponse addItem(UUID userId, AddCartItemRequest request) {
        List<CartItemData> items = cartRepository.getItems(userId);

        CartItemData existing = items.stream()
                .filter(i -> i.bookId().equals(request.bookId()) && i.deliveryType() == request.deliveryType())
                .findFirst()
                .orElse(null);

        int newQty = (existing != null ? existing.qty() : 0) + request.qty();
        if (newQty > MAX_QTY_PER_TITLE) {
            throw new QtyLimitExceededException();
        }

        BookInfo book = catalogClient.getBook(request.bookId());
        BigDecimal unitPrice = validateAvailability(book, request.deliveryType(), newQty);

        CartItemData updated = new CartItemData(book.id(), book.title(), newQty, unitPrice, request.deliveryType(), Instant.now());
        items.removeIf(i -> i.bookId().equals(request.bookId()) && i.deliveryType() == request.deliveryType());
        items.add(updated);
        cartRepository.saveItems(userId, items);

        return toSummary(items);
    }

    public CartSummaryResponse setItemQty(UUID userId, UUID bookId, DeliveryType deliveryType, int qty) {
        List<CartItemData> items = cartRepository.getItems(userId);

        boolean exists = items.stream().anyMatch(i -> i.bookId().equals(bookId) && i.deliveryType() == deliveryType);
        if (!exists) {
            throw new CartItemNotFoundException();
        }

        if (qty == 0) {
            items.removeIf(i -> i.bookId().equals(bookId) && i.deliveryType() == deliveryType);
        } else {
            if (qty > MAX_QTY_PER_TITLE) {
                throw new QtyLimitExceededException();
            }
            BookInfo book = catalogClient.getBook(bookId);
            validateAvailability(book, deliveryType, qty);
            items.replaceAll(i -> i.bookId().equals(bookId) && i.deliveryType() == deliveryType ? i.withQty(qty) : i);
        }

        cartRepository.saveItems(userId, items);
        return toSummary(items);
    }

    /** @return the unit price to snapshot for this delivery type */
    private BigDecimal validateAvailability(BookInfo book, DeliveryType deliveryType, int qty) {
        if (deliveryType == DeliveryType.VIRTUAL) {
            if (book.virtualEdition() == null) {
                throw new VirtualEditionNotAvailableException(book.id());
            }
            return book.virtualEdition().price();
        }

        if (book.availability() == null || book.availability().quantityAvailable() < qty) {
            throw new InsufficientStockException("Requested quantity exceeds available inventory");
        }
        return book.listPrice();
    }

    private CartResponse toResponse(List<CartItemData> items) {
        List<CartResponse.Item> responseItems = items.stream()
                .map(i -> new CartResponse.Item(
                        i.bookId(), i.title(), i.qty(), i.unitPriceSnapshot(),
                        i.unitPriceSnapshot().multiply(BigDecimal.valueOf(i.qty())), i.deliveryType()
                ))
                .toList();

        BigDecimal subtotal = responseItems.stream()
                .map(CartResponse.Item::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int itemCount = items.stream().mapToInt(CartItemData::qty).sum();
        boolean requiresShippingAddress = items.stream().anyMatch(i -> i.deliveryType() == DeliveryType.PHYSICAL);

        return new CartResponse(responseItems, subtotal, "INR", itemCount, requiresShippingAddress);
    }

    private CartSummaryResponse toSummary(List<CartItemData> items) {
        BigDecimal subtotal = items.stream()
                .map(i -> i.unitPriceSnapshot().multiply(BigDecimal.valueOf(i.qty())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int itemCount = items.stream().mapToInt(CartItemData::qty).sum();
        return new CartSummaryResponse(itemCount, subtotal, "INR");
    }
}
