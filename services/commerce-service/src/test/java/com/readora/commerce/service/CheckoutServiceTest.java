package com.readora.commerce.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.readora.commerce.cart.CartRepository;
import com.readora.commerce.client.CatalogClient;
import com.readora.commerce.client.UserServiceClient;
import com.readora.commerce.dto.CheckoutRequest;
import com.readora.commerce.dto.CheckoutResponse;
import com.readora.commerce.dto.ReserveStockResponse;
import com.readora.commerce.dto.StoreInfo;
import com.readora.commerce.dto.VirtualEditionLookupResponse;
import com.readora.commerce.dto.WalletBalance;
import com.readora.commerce.entity.DeliveryType;
import com.readora.commerce.entity.Order;
import com.readora.commerce.entity.OrderStatus;
import com.readora.commerce.entity.PaymentMethod;
import com.readora.commerce.exception.CartEmptyException;
import com.readora.commerce.exception.InsufficientWalletBalanceException;
import com.readora.commerce.exception.InvalidPaymentMethodException;
import com.readora.commerce.exception.MultipleStoresInCartException;
import com.readora.commerce.exception.ShippingAddressCityMismatchException;
import com.readora.commerce.exception.ShippingAddressRequiredException;
import com.readora.commerce.exception.VirtualEditionAlreadyOwnedException;
import com.readora.commerce.exception.VirtualEditionNotAvailableException;
import com.readora.commerce.repository.OrderItemRepository;
import com.readora.commerce.repository.OrderRepository;
import com.readora.commerce.repository.OrderShippingAddressRepository;
import com.readora.commerce.repository.OrderStatusHistoryRepository;
import com.readora.commerce.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckoutServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private OrderShippingAddressRepository shippingAddressRepository;
    @Mock private OrderStatusHistoryRepository historyRepository;
    @Mock private OutboxEventRepository outboxEventRepository;
    @Mock private CatalogClient catalogClient;
    @Mock private UserServiceClient userServiceClient;
    @Mock private CartRepository cartRepository;

    private CheckoutService checkoutService;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        OrderEventRecorder eventRecorder = new OrderEventRecorder(historyRepository, outboxEventRepository, new ObjectMapper());
        checkoutService = new CheckoutService(
                orderRepository, orderItemRepository, shippingAddressRepository, catalogClient,
                userServiceClient, cartRepository, eventRecorder
        );
    }

    private static Order newOrder(OrderStatus status, DeliveryType deliveryType) {
        Order order = new Order(
                "RDA-2026-000001", UUID.randomUUID(), "INR", new BigDecimal("100.00"), BigDecimal.ZERO,
                BigDecimal.ZERO, new BigDecimal("9.00"), new BigDecimal("109.00"),
                BigDecimal.ZERO, PaymentMethod.WALLET, UUID.randomUUID().toString(), deliveryType
        );
        ReflectionTestUtils.setField(order, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(order, "placedAt", Instant.now());
        order.transitionTo(status);
        return order;
    }

    private static CheckoutRequest.Item physicalItem(UUID bookId) {
        return new CheckoutRequest.Item(bookId, 1, DeliveryType.PHYSICAL);
    }

    private static CheckoutRequest.Item virtualItem(UUID bookId) {
        return new CheckoutRequest.Item(bookId, 1, DeliveryType.VIRTUAL);
    }

    private static CheckoutRequest.ShippingAddress address(String city) {
        return new CheckoutRequest.ShippingAddress("Name", "Line 1", null, city, "State", "000000", "IN", "999");
    }

    @Test
    void checkout_emptyCart_throws() {
        assertThatThrownBy(() -> checkoutService.checkout(userId, "idem-1", new CheckoutRequest(null, "WALLET", null, List.of())))
                .isInstanceOf(CartEmptyException.class);
    }

    @Test
    void checkout_virtualItemAlreadyOwned_throwsBeforeAnyReservation() {
        UUID bookId = UUID.randomUUID();
        when(orderItemRepository.existsActiveVirtualPurchase(userId, bookId)).thenReturn(true);

        assertThatThrownBy(() -> checkoutService.checkout(userId, "idem-1",
                new CheckoutRequest(null, "WALLET", null, List.of(virtualItem(bookId)))))
                .isInstanceOf(VirtualEditionAlreadyOwnedException.class);

        verify(catalogClient, never()).lookupVirtualEditions(any());
    }

    @Test
    void checkout_physicalItemsWithoutShippingAddress_throws() {
        UUID bookId = UUID.randomUUID();

        assertThatThrownBy(() -> checkoutService.checkout(userId, "idem-1",
                new CheckoutRequest(null, "WALLET", null, List.of(physicalItem(bookId)))))
                .isInstanceOf(ShippingAddressRequiredException.class);
    }

    @Test
    void checkout_idempotentReplay_returnsExistingOrderWithoutReReserving() {
        UUID bookId = UUID.randomUUID();
        Order existing = newOrder(OrderStatus.PENDING_PAYMENT, DeliveryType.VIRTUAL);
        when(orderItemRepository.existsActiveVirtualPurchase(any(), any())).thenReturn(false);
        when(orderRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.of(existing));

        CheckoutResponse response = checkoutService.checkout(userId, "idem-1",
                new CheckoutRequest(null, "WALLET", null, List.of(virtualItem(bookId))));

        assertThat(response.orderId()).isEqualTo(existing.getId());
        verify(catalogClient, never()).lookupVirtualEditions(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void checkout_unsupportedPaymentMethod_throws() {
        UUID bookId = UUID.randomUUID();
        when(orderItemRepository.existsActiveVirtualPurchase(any(), any())).thenReturn(false);
        when(orderRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(catalogClient.lookupVirtualEditions(any())).thenReturn(new VirtualEditionLookupResponse(
                List.of(new VirtualEditionLookupResponse.Item(bookId, "Title", true, new BigDecimal("50.00"), "INR"))));

        assertThatThrownBy(() -> checkoutService.checkout(userId, "idem-1",
                new CheckoutRequest(null, "CREDIT_CARD", null, List.of(virtualItem(bookId)))))
                .isInstanceOf(InvalidPaymentMethodException.class);
    }

    @Test
    void checkout_insufficientWalletBalance_throws() {
        UUID bookId = UUID.randomUUID();
        when(orderItemRepository.existsActiveVirtualPurchase(any(), any())).thenReturn(false);
        when(orderRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(catalogClient.lookupVirtualEditions(any())).thenReturn(new VirtualEditionLookupResponse(
                List.of(new VirtualEditionLookupResponse.Item(bookId, "Title", true, new BigDecimal("500.00"), "INR"))));
        when(userServiceClient.getWalletBalance(userId)).thenReturn(new WalletBalance(new BigDecimal("10.00"), "INR"));

        assertThatThrownBy(() -> checkoutService.checkout(userId, "idem-1",
                new CheckoutRequest(null, "WALLET", null, List.of(virtualItem(bookId)))))
                .isInstanceOf(InsufficientWalletBalanceException.class);
    }

    @Test
    void checkout_virtualEditionUnavailable_throws() {
        UUID bookId = UUID.randomUUID();
        when(orderItemRepository.existsActiveVirtualPurchase(any(), any())).thenReturn(false);
        when(orderRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(catalogClient.lookupVirtualEditions(any())).thenReturn(new VirtualEditionLookupResponse(
                List.of(new VirtualEditionLookupResponse.Item(bookId, "Title", false, null, null))));

        assertThatThrownBy(() -> checkoutService.checkout(userId, "idem-1",
                new CheckoutRequest(null, "WALLET", null, List.of(virtualItem(bookId)))))
                .isInstanceOf(VirtualEditionNotAvailableException.class);
    }

    @Test
    void checkout_virtualOnly_success_skipsShippingAndStoreLookup() {
        UUID bookId = UUID.randomUUID();
        when(orderItemRepository.existsActiveVirtualPurchase(any(), any())).thenReturn(false);
        when(orderRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(catalogClient.lookupVirtualEditions(any())).thenReturn(new VirtualEditionLookupResponse(
                List.of(new VirtualEditionLookupResponse.Item(bookId, "Title", true, new BigDecimal("50.00"), "INR"))));

        CheckoutResponse response = checkoutService.checkout(userId, "idem-1",
                new CheckoutRequest(null, "UPI", null, List.of(virtualItem(bookId))));

        assertThat(response.deliveryType()).isEqualTo("VIRTUAL");
        verify(catalogClient, never()).getStore(any());
        verify(cartRepository).clear(userId);
        verify(outboxEventRepository, org.mockito.Mockito.atLeastOnce()).save(any());
    }

    @Test
    void checkout_physicalShippingCityMismatch_throws() {
        UUID bookId = UUID.randomUUID();
        when(orderRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        UUID storeId = UUID.randomUUID();
        when(catalogClient.reserveStock(any())).thenReturn(new ReserveStockResponse(
                List.of(new ReserveStockResponse.Item(bookId, "Title", "9780000000001", new BigDecimal("100.00"), storeId))));
        when(catalogClient.getStore(storeId)).thenReturn(new StoreInfo(storeId, "Mumbai"));

        assertThatThrownBy(() -> checkoutService.checkout(userId, "idem-1",
                new CheckoutRequest(address("Delhi"), "UPI", null, List.of(physicalItem(bookId)))))
                .isInstanceOf(ShippingAddressCityMismatchException.class);
    }

    @Test
    void checkout_physical_success_appliesFreeShippingAboveThreshold() {
        UUID bookId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        when(orderRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(catalogClient.reserveStock(any())).thenReturn(new ReserveStockResponse(
                List.of(new ReserveStockResponse.Item(bookId, "Title", "9780000000001", new BigDecimal("999.00"), storeId))));
        when(catalogClient.getStore(storeId)).thenReturn(new StoreInfo(storeId, "Mumbai"));

        CheckoutResponse response = checkoutService.checkout(userId, "idem-1",
                new CheckoutRequest(address("Mumbai"), "UPI", null, List.of(physicalItem(bookId))));

        assertThat(response.shippingFee()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.packagingFee()).isEqualByComparingTo("15.00");
        verify(shippingAddressRepository).save(any());
    }

    @Test
    void checkout_physical_success_belowThresholdChargesFlatShipping() {
        UUID bookId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        when(orderRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(catalogClient.reserveStock(any())).thenReturn(new ReserveStockResponse(
                List.of(new ReserveStockResponse.Item(bookId, "Title", "9780000000001", new BigDecimal("100.00"), storeId))));
        when(catalogClient.getStore(storeId)).thenReturn(new StoreInfo(storeId, "Mumbai"));

        CheckoutResponse response = checkoutService.checkout(userId, "idem-1",
                new CheckoutRequest(address("Mumbai"), "UPI", null, List.of(physicalItem(bookId))));

        assertThat(response.shippingFee()).isEqualByComparingTo("40.00");
    }

    @Test
    void checkout_multiplePhysicalStores_throws() {
        UUID bookId1 = UUID.randomUUID();
        UUID bookId2 = UUID.randomUUID();
        when(orderRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(catalogClient.reserveStock(any())).thenReturn(new ReserveStockResponse(List.of(
                new ReserveStockResponse.Item(bookId1, "T1", "9780000000001", BigDecimal.TEN, UUID.randomUUID()),
                new ReserveStockResponse.Item(bookId2, "T2", "9780000000002", BigDecimal.TEN, UUID.randomUUID()))));

        assertThatThrownBy(() -> checkoutService.checkout(userId, "idem-1",
                new CheckoutRequest(address("Mumbai"), "UPI", null, List.of(physicalItem(bookId1), physicalItem(bookId2)))))
                .isInstanceOf(MultipleStoresInCartException.class);
    }

    @Test
    void checkout_physicalWallet_usesGrandTotalAsWalletAmount() {
        UUID bookId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        when(orderRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(catalogClient.reserveStock(any())).thenReturn(new ReserveStockResponse(
                List.of(new ReserveStockResponse.Item(bookId, "Title", "9780000000001", new BigDecimal("100.00"), storeId))));
        when(catalogClient.getStore(storeId)).thenReturn(new StoreInfo(storeId, "Mumbai"));
        when(userServiceClient.getWalletBalance(userId)).thenReturn(new WalletBalance(new BigDecimal("1000.00"), "INR"));

        CheckoutResponse response = checkoutService.checkout(userId, "idem-1",
                new CheckoutRequest(address("Mumbai"), "WALLET", null, List.of(physicalItem(bookId))));

        assertThat(response.walletAmountUsed()).isEqualByComparingTo(response.grandTotal());
    }
}
