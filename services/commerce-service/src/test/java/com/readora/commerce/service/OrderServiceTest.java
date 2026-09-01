package com.readora.commerce.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.readora.commerce.cart.CartRepository;
import com.readora.commerce.client.CatalogClient;
import com.readora.commerce.client.PaymentClient;
import com.readora.commerce.client.UserServiceClient;
import com.readora.commerce.dto.CancelOrderRequest;
import com.readora.commerce.dto.CheckoutRequest;
import com.readora.commerce.dto.CheckoutResponse;
import com.readora.commerce.dto.ReserveStockResponse;
import com.readora.commerce.dto.ReturnOrderRequest;
import com.readora.commerce.dto.StoreInfo;
import com.readora.commerce.dto.VirtualEditionLookupResponse;
import com.readora.commerce.dto.WalletBalance;
import com.readora.commerce.entity.DeliveryType;
import com.readora.commerce.entity.Order;
import com.readora.commerce.entity.OrderStatus;
import com.readora.commerce.exception.CartEmptyException;
import com.readora.commerce.exception.InsufficientWalletBalanceException;
import com.readora.commerce.exception.InvalidDeliveryTransitionException;
import com.readora.commerce.exception.InvalidPaymentMethodException;
import com.readora.commerce.exception.InvalidReturnTransitionException;
import com.readora.commerce.exception.OrderAlreadyCancelledException;
import com.readora.commerce.exception.OrderAlreadyShippedException;
import com.readora.commerce.exception.OrderCancelWindowExpiredException;
import com.readora.commerce.exception.OrderNotFoundException;
import com.readora.commerce.exception.OrderNotReturnableException;
import com.readora.commerce.exception.ReturnNotUnderReviewException;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Duration;
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
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private OrderShippingAddressRepository shippingAddressRepository;
    @Mock private OrderStatusHistoryRepository historyRepository;
    @Mock private OutboxEventRepository outboxEventRepository;
    @Mock private CatalogClient catalogClient;
    @Mock private UserServiceClient userServiceClient;
    @Mock private PaymentClient paymentClient;
    @Mock private CartRepository cartRepository;
    @Mock private ReturnMessageService returnMessageService;

    private OrderService orderService;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        orderService = new OrderService(
                orderRepository, orderItemRepository, shippingAddressRepository, historyRepository,
                outboxEventRepository, catalogClient, userServiceClient, paymentClient, cartRepository,
                new ObjectMapper(), returnMessageService
        );
    }

    private static Order newOrder(OrderStatus status, DeliveryType deliveryType) {
        Order order = new Order(
                "RDA-2026-000001", UUID.randomUUID(), "INR", new BigDecimal("100.00"), BigDecimal.ZERO,
                BigDecimal.ZERO, new BigDecimal("9.00"), new BigDecimal("109.00"),
                BigDecimal.ZERO, "WALLET", UUID.randomUUID().toString(), deliveryType
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

    // ---- checkout ----

    @Test
    void checkout_emptyCart_throws() {
        assertThatThrownBy(() -> orderService.checkout(userId, "idem-1", new CheckoutRequest(null, "WALLET", null, List.of())))
                .isInstanceOf(CartEmptyException.class);
    }

    @Test
    void checkout_virtualItemAlreadyOwned_throwsBeforeAnyReservation() {
        UUID bookId = UUID.randomUUID();
        when(orderItemRepository.existsActiveVirtualPurchase(userId, bookId)).thenReturn(true);

        assertThatThrownBy(() -> orderService.checkout(userId, "idem-1",
                new CheckoutRequest(null, "WALLET", null, List.of(virtualItem(bookId)))))
                .isInstanceOf(VirtualEditionAlreadyOwnedException.class);

        verify(catalogClient, never()).lookupVirtualEditions(any());
    }

    @Test
    void checkout_physicalItemsWithoutShippingAddress_throws() {
        UUID bookId = UUID.randomUUID();

        assertThatThrownBy(() -> orderService.checkout(userId, "idem-1",
                new CheckoutRequest(null, "WALLET", null, List.of(physicalItem(bookId)))))
                .isInstanceOf(ShippingAddressRequiredException.class);
    }

    @Test
    void checkout_idempotentReplay_returnsExistingOrderWithoutReReserving() {
        UUID bookId = UUID.randomUUID();
        Order existing = newOrder(OrderStatus.PENDING_PAYMENT, DeliveryType.VIRTUAL);
        when(orderItemRepository.existsActiveVirtualPurchase(any(), any())).thenReturn(false);
        when(orderRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.of(existing));

        CheckoutResponse response = orderService.checkout(userId, "idem-1",
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

        assertThatThrownBy(() -> orderService.checkout(userId, "idem-1",
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

        assertThatThrownBy(() -> orderService.checkout(userId, "idem-1",
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

        assertThatThrownBy(() -> orderService.checkout(userId, "idem-1",
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

        CheckoutResponse response = orderService.checkout(userId, "idem-1",
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

        assertThatThrownBy(() -> orderService.checkout(userId, "idem-1",
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

        CheckoutResponse response = orderService.checkout(userId, "idem-1",
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

        CheckoutResponse response = orderService.checkout(userId, "idem-1",
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

        assertThatThrownBy(() -> orderService.checkout(userId, "idem-1",
                new CheckoutRequest(address("Mumbai"), "UPI", null, List.of(physicalItem(bookId1), physicalItem(bookId2)))))
                .isInstanceOf(com.readora.commerce.exception.MultipleStoresInCartException.class);
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

        CheckoutResponse response = orderService.checkout(userId, "idem-1",
                new CheckoutRequest(address("Mumbai"), "WALLET", null, List.of(physicalItem(bookId))));

        assertThat(response.walletAmountUsed()).isEqualByComparingTo(response.grandTotal());
    }

    // ---- listOrders / getDetail ----

    @Test
    void listOrders_mapsOrdersWithCoverImages() {
        Order order = newOrder(OrderStatus.CONFIRMED, DeliveryType.PHYSICAL);
        com.readora.commerce.entity.OrderItem item = new com.readora.commerce.entity.OrderItem(
                order, UUID.randomUUID(), "Title", "9780000000001", BigDecimal.TEN, 1, DeliveryType.PHYSICAL);
        when(orderRepository.findAllByUserIdOrderByPlacedAtDesc(any(), any())).thenReturn(new PageImpl<>(List.of(order)));
        when(orderItemRepository.findAllByOrderIdIn(any())).thenReturn(List.of(item));
        when(catalogClient.getCoverImageUrls(any())).thenReturn(java.util.Map.of(item.getBookId(), "http://cover.jpg"));

        var page = orderService.listOrders(userId, Pageable.unpaged());

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).itemPreviews()).hasSize(1);
    }

    @Test
    void getDetail_notFound_throws() {
        when(orderRepository.findByIdAndUserId(any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getDetail(userId, UUID.randomUUID()))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void getDetail_found_mapsWithNoPaymentRecordYet() {
        Order order = newOrder(OrderStatus.PENDING_PAYMENT, DeliveryType.VIRTUAL);
        when(orderRepository.findByIdAndUserId(order.getId(), userId)).thenReturn(Optional.of(order));
        when(orderItemRepository.findAllByOrderId(any())).thenReturn(List.of());
        when(shippingAddressRepository.findByOrderId(any())).thenReturn(Optional.empty());
        when(historyRepository.findAllByOrderIdOrderByChangedAt(any())).thenReturn(List.of());
        when(paymentClient.getPaymentDetails(any())).thenReturn(Optional.empty());

        var detail = orderService.getDetail(userId, order.getId());

        assertThat(detail.payment()).isNull();
    }

    @Test
    void getDetail_found_mapsItemsAddressAndPayment() {
        Order order = newOrder(OrderStatus.CONFIRMED, DeliveryType.PHYSICAL);
        com.readora.commerce.entity.OrderItem item = new com.readora.commerce.entity.OrderItem(
                order, UUID.randomUUID(), "Title", "9780000000001", BigDecimal.TEN, 1, DeliveryType.PHYSICAL);
        com.readora.commerce.entity.OrderShippingAddress address = new com.readora.commerce.entity.OrderShippingAddress(
                order, "Name", "Line 1", null, "Mumbai", "MH", "400001", "IN", "999");
        when(orderRepository.findByIdAndUserId(order.getId(), userId)).thenReturn(Optional.of(order));
        when(orderItemRepository.findAllByOrderId(any())).thenReturn(List.of(item));
        when(shippingAddressRepository.findByOrderId(any())).thenReturn(Optional.of(address));
        when(historyRepository.findAllByOrderIdOrderByChangedAt(any())).thenReturn(List.of());
        when(paymentClient.getPaymentDetails(any())).thenReturn(Optional.of(
                new com.readora.commerce.dto.PaymentDetails(UUID.randomUUID(), order.getId(), "CAPTURED", "UPI",
                        order.getGrandTotal(), BigDecimal.ZERO, Instant.now(), Instant.now())));

        var detail = orderService.getDetail(userId, order.getId());

        assertThat(detail.items()).hasSize(1);
        assertThat(detail.shippingAddress().city()).isEqualTo("Mumbai");
        assertThat(detail.payment().status()).isEqualTo("CAPTURED");
    }

    // ---- cancel ----

    @Test
    void cancel_notFound_throws() {
        when(orderRepository.findByIdAndUserId(any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.cancel(userId, UUID.randomUUID(), new CancelOrderRequest("changed mind")))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void cancel_alreadyCancelled_throws() {
        Order order = newOrder(OrderStatus.CANCELLED, DeliveryType.PHYSICAL);
        when(orderRepository.findByIdAndUserId(order.getId(), userId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancel(userId, order.getId(), new CancelOrderRequest("x")))
                .isInstanceOf(OrderAlreadyCancelledException.class);
    }

    @Test
    void cancel_alreadyShipped_throws() {
        Order order = newOrder(OrderStatus.SHIPPED, DeliveryType.PHYSICAL);
        when(orderRepository.findByIdAndUserId(order.getId(), userId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancel(userId, order.getId(), new CancelOrderRequest("x")))
                .isInstanceOf(OrderAlreadyShippedException.class);
    }

    @Test
    void cancel_windowExpired_throws() {
        Order order = newOrder(OrderStatus.CONFIRMED, DeliveryType.PHYSICAL);
        ReflectionTestUtils.setField(order, "placedAt", Instant.now().minus(Duration.ofHours(49)));
        when(orderRepository.findByIdAndUserId(order.getId(), userId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancel(userId, order.getId(), new CancelOrderRequest("x")))
                .isInstanceOf(OrderCancelWindowExpiredException.class);
    }

    @Test
    void cancel_valid_marksCancelledAndPublishesEvent() {
        Order order = newOrder(OrderStatus.CONFIRMED, DeliveryType.PHYSICAL);
        when(orderRepository.findByIdAndUserId(order.getId(), userId)).thenReturn(Optional.of(order));

        var response = orderService.cancel(userId, order.getId(), new CancelOrderRequest("changed mind"));

        assertThat(response.status()).isEqualTo("CANCELLED");
        verify(historyRepository).save(any());
        verify(outboxEventRepository, org.mockito.Mockito.atLeastOnce()).save(any());
    }

    // ---- returnOrder ----

    @Test
    void returnOrder_notReturnable_throws() {
        Order order = newOrder(OrderStatus.CONFIRMED, DeliveryType.PHYSICAL);
        when(orderRepository.findByIdAndUserId(order.getId(), userId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.returnOrder(userId, order.getId(), new ReturnOrderRequest("defective")))
                .isInstanceOf(OrderNotReturnableException.class);
    }

    @Test
    void returnOrder_virtual_autoInitiatesRefundWithoutAdminNotification() {
        Order order = newOrder(OrderStatus.DELIVERED, DeliveryType.VIRTUAL);
        ReflectionTestUtils.setField(order, "deliveredAt", Instant.now().minus(Duration.ofHours(1)));
        when(orderRepository.findByIdAndUserId(order.getId(), userId)).thenReturn(Optional.of(order));

        orderService.returnOrder(userId, order.getId(), new ReturnOrderRequest("not what I wanted"));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.REFUND_INITIATED);
        verify(userServiceClient, never()).getAdminUserIdForStore(any());
    }

    @Test
    void returnOrder_physical_notifiesStoreAdminAndStaysAtRequested() {
        Order order = newOrder(OrderStatus.DELIVERED, DeliveryType.PHYSICAL);
        ReflectionTestUtils.setField(order, "deliveredAt", Instant.now().minus(Duration.ofHours(1)));
        UUID storeId = UUID.randomUUID();
        order.setStoreId(storeId);
        when(orderRepository.findByIdAndUserId(order.getId(), userId)).thenReturn(Optional.of(order));
        when(userServiceClient.getAdminUserIdForStore(storeId)).thenReturn(UUID.randomUUID());

        orderService.returnOrder(userId, order.getId(), new ReturnOrderRequest("damaged"));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.RETURN_REQUESTED);
    }

    @Test
    void returnOrder_physical_noAdminAssignedToStore_isANoOp() {
        Order order = newOrder(OrderStatus.DELIVERED, DeliveryType.PHYSICAL);
        ReflectionTestUtils.setField(order, "deliveredAt", Instant.now().minus(Duration.ofHours(1)));
        when(orderRepository.findByIdAndUserId(order.getId(), userId)).thenReturn(Optional.of(order));
        when(userServiceClient.getAdminUserIdForStore(any())).thenReturn(null);

        orderService.returnOrder(userId, order.getId(), new ReturnOrderRequest("damaged"));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.RETURN_REQUESTED);
    }

    // ---- reviewReturn ----

    @Test
    void reviewReturn_notUnderReview_throws() {
        Order order = newOrder(OrderStatus.DELIVERED, DeliveryType.PHYSICAL);
        UUID storeId = UUID.randomUUID();
        when(orderRepository.findByIdAndStoreId(order.getId(), storeId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.reviewReturn(UUID.randomUUID(), order.getId(), storeId, "APPROVE", "ok"))
                .isInstanceOf(ReturnNotUnderReviewException.class);
    }

    @Test
    void reviewReturn_approve_transitionsToApproved() {
        Order order = newOrder(OrderStatus.RETURN_REQUESTED, DeliveryType.PHYSICAL);
        UUID storeId = UUID.randomUUID();
        when(orderRepository.findByIdAndStoreId(order.getId(), storeId)).thenReturn(Optional.of(order));

        orderService.reviewReturn(UUID.randomUUID(), order.getId(), storeId, "approve", "looks good");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.RETURN_APPROVED);
    }

    @Test
    void reviewReturn_reject_transitionsToRejected() {
        Order order = newOrder(OrderStatus.RETURN_REQUESTED, DeliveryType.PHYSICAL);
        UUID storeId = UUID.randomUUID();
        when(orderRepository.findByIdAndStoreId(order.getId(), storeId)).thenReturn(Optional.of(order));

        orderService.reviewReturn(UUID.randomUUID(), order.getId(), storeId, "REJECT", "no receipt");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.RETURN_REJECTED);
    }

    @Test
    void reviewReturn_invalidDecision_throws() {
        Order order = newOrder(OrderStatus.RETURN_REQUESTED, DeliveryType.PHYSICAL);
        UUID storeId = UUID.randomUUID();
        when(orderRepository.findByIdAndStoreId(order.getId(), storeId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.reviewReturn(UUID.randomUUID(), order.getId(), storeId, "MAYBE", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---- updateReturnPickupStatus ----

    @Test
    void updateReturnPickupStatus_illegalTransition_throws() {
        Order order = newOrder(OrderStatus.RETURN_REQUESTED, DeliveryType.PHYSICAL);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateReturnPickupStatus(
                order.getId(), OrderStatus.RETURN_COLLECTED, UUID.randomUUID(), "Agent"))
                .isInstanceOf(InvalidReturnTransitionException.class);
    }

    @Test
    void updateReturnPickupStatus_enRoute_transitions() {
        Order order = newOrder(OrderStatus.RETURN_ASSIGNED, DeliveryType.PHYSICAL);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        orderService.updateReturnPickupStatus(order.getId(), OrderStatus.RETURN_EN_ROUTE, UUID.randomUUID(), "Agent");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.RETURN_EN_ROUTE);
    }

    @Test
    void updateReturnPickupStatus_collected_initiatesRefund() {
        Order order = newOrder(OrderStatus.RETURN_EN_ROUTE, DeliveryType.PHYSICAL);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        orderService.updateReturnPickupStatus(order.getId(), OrderStatus.RETURN_COLLECTED, UUID.randomUUID(), "Agent");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.REFUND_INITIATED);
    }

    @Test
    void updateReturnPickupStatus_assigned_setsAgentSnapshot() {
        Order order = newOrder(OrderStatus.RETURN_APPROVED, DeliveryType.PHYSICAL);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        orderService.updateReturnPickupStatus(order.getId(), OrderStatus.RETURN_ASSIGNED, UUID.randomUUID(), "Agent Smith");

        assertThat(order.getReturnAgentName()).isEqualTo("Agent Smith");
    }

    // ---- handleRefundCompleted ----

    @Test
    void handleRefundCompleted_wrongStatus_isANoOp() {
        Order order = newOrder(OrderStatus.DELIVERED, DeliveryType.PHYSICAL);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        orderService.handleRefundCompleted(order.getId());

        verify(orderRepository, never()).save(any());
    }

    @Test
    void handleRefundCompleted_success_marksReturned() {
        Order order = newOrder(OrderStatus.REFUND_INITIATED, DeliveryType.PHYSICAL);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        orderService.handleRefundCompleted(order.getId());

        assertThat(order.getStatus()).isEqualTo(OrderStatus.RETURNED);
    }

    @Test
    void handleRefundCompleted_orderNotFound_isANoOp() {
        when(orderRepository.findById(any())).thenReturn(Optional.empty());

        orderService.handleRefundCompleted(UUID.randomUUID());

        verify(orderRepository, never()).save(any());
    }

    // ---- handlePaymentCaptured ----

    @Test
    void handlePaymentCaptured_wrongStatus_isANoOp() {
        Order order = newOrder(OrderStatus.CONFIRMED, DeliveryType.PHYSICAL);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        orderService.handlePaymentCaptured(order.getId());

        verify(orderRepository, never()).save(any());
    }

    @Test
    void handlePaymentCaptured_physical_stopsAtConfirmed() {
        Order order = newOrder(OrderStatus.PENDING_PAYMENT, DeliveryType.PHYSICAL);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        orderService.handlePaymentCaptured(order.getId());

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void handlePaymentCaptured_virtual_autoAdvancesToDelivered() {
        Order order = newOrder(OrderStatus.PENDING_PAYMENT, DeliveryType.VIRTUAL);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        orderService.handlePaymentCaptured(order.getId());

        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
        assertThat(order.getDeliveredAt()).isNotNull();
    }

    // ---- updateDeliveryStatus ----

    @Test
    void updateDeliveryStatus_illegalTransition_throws() {
        Order order = newOrder(OrderStatus.CONFIRMED, DeliveryType.PHYSICAL);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateDeliveryStatus(order.getId(), OrderStatus.DELIVERED, UUID.randomUUID(), "Agent"))
                .isInstanceOf(InvalidDeliveryTransitionException.class);
    }

    @Test
    void updateDeliveryStatus_assignedFromConfirmed_setsAgentSnapshot() {
        Order order = newOrder(OrderStatus.CONFIRMED, DeliveryType.PHYSICAL);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        orderService.updateDeliveryStatus(order.getId(), OrderStatus.ASSIGNED, UUID.randomUUID(), "Agent Smith");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.ASSIGNED);
        assertThat(order.getDeliveryAgentName()).isEqualTo("Agent Smith");
    }

    @Test
    void updateDeliveryStatus_shippedFromAssigned_transitions() {
        Order order = newOrder(OrderStatus.ASSIGNED, DeliveryType.PHYSICAL);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        orderService.updateDeliveryStatus(order.getId(), OrderStatus.SHIPPED, UUID.randomUUID(), "Agent");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPED);
    }

    @Test
    void updateDeliveryStatus_deliveredFromShipped_marksDeliveredAt() {
        Order order = newOrder(OrderStatus.SHIPPED, DeliveryType.PHYSICAL);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        orderService.updateDeliveryStatus(order.getId(), OrderStatus.DELIVERED, UUID.randomUUID(), "Agent");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
        assertThat(order.getDeliveredAt()).isNotNull();
    }

    // ---- handlePaymentFailed ----

    @Test
    void handlePaymentFailed_wrongStatus_isANoOp() {
        Order order = newOrder(OrderStatus.CONFIRMED, DeliveryType.PHYSICAL);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        orderService.handlePaymentFailed(order.getId());

        verify(orderRepository, never()).save(any());
    }

    @Test
    void handlePaymentFailed_pending_marksFailed() {
        Order order = newOrder(OrderStatus.PENDING_PAYMENT, DeliveryType.PHYSICAL);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        orderService.handlePaymentFailed(order.getId());

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYMENT_FAILED);
    }

    // ---- return messages / delivery detail ----

    @Test
    void listReturnMessages_delegatesToReturnMessageService() {
        Order order = newOrder(OrderStatus.RETURN_REQUESTED, DeliveryType.PHYSICAL);
        when(orderRepository.findByIdAndUserId(order.getId(), userId)).thenReturn(Optional.of(order));
        when(returnMessageService.list(order.getId())).thenReturn(List.of());

        assertThat(orderService.listReturnMessages(userId, order.getId())).isEmpty();
    }

    @Test
    void getDeliveryDetail_notFound_throws() {
        when(orderRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getDeliveryDetail(UUID.randomUUID())).isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void getDeliveryDetail_found_mapsItemsAndAddress() {
        Order order = newOrder(OrderStatus.CONFIRMED, DeliveryType.PHYSICAL);
        com.readora.commerce.entity.OrderItem item = new com.readora.commerce.entity.OrderItem(
                order, UUID.randomUUID(), "Title", "9780000000001", BigDecimal.TEN, 2, DeliveryType.PHYSICAL);
        com.readora.commerce.entity.OrderShippingAddress address = new com.readora.commerce.entity.OrderShippingAddress(
                order, "Name", "Line 1", "Line 2", "Mumbai", "MH", "400001", "IN", "999");
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(orderItemRepository.findAllByOrderId(any())).thenReturn(List.of(item));
        when(shippingAddressRepository.findByOrderId(any())).thenReturn(Optional.of(address));

        var detail = orderService.getDeliveryDetail(order.getId());

        assertThat(detail.items()).hasSize(1);
        assertThat(detail.shippingAddress().city()).isEqualTo("Mumbai");
    }

    // ---- postReturnMessage ----

    @Test
    void postReturnMessage_delegatesToReturnMessageServiceAsCustomer() {
        Order order = newOrder(OrderStatus.RETURN_REQUESTED, DeliveryType.PHYSICAL);
        when(orderRepository.findByIdAndUserId(order.getId(), userId)).thenReturn(Optional.of(order));

        orderService.postReturnMessage(userId, order.getId(), "Any update?");

        verify(returnMessageService).post(order, userId, com.readora.commerce.entity.ReturnSenderRole.CUSTOMER, "Any update?");
    }

    @Test
    void postReturnMessage_orderNotFound_throws() {
        when(orderRepository.findByIdAndUserId(any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.postReturnMessage(userId, UUID.randomUUID(), "hi"))
                .isInstanceOf(OrderNotFoundException.class);
    }
}
