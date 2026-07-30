package com.readora.commerce.service;

import com.readora.commerce.client.CatalogClient;
import com.readora.commerce.client.PaymentClient;
import com.readora.commerce.dto.PaymentDetails;
import com.readora.commerce.entity.DeliveryType;
import com.readora.commerce.entity.Order;
import com.readora.commerce.entity.OrderItem;
import com.readora.commerce.entity.OrderShippingAddress;
import com.readora.commerce.entity.OrderStatus;
import com.readora.commerce.entity.PaymentMethod;
import com.readora.commerce.exception.OrderNotFoundException;
import com.readora.commerce.repository.OrderItemRepository;
import com.readora.commerce.repository.OrderRepository;
import com.readora.commerce.repository.OrderShippingAddressRepository;
import com.readora.commerce.repository.OrderStatusHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderQueryServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private OrderShippingAddressRepository shippingAddressRepository;
    @Mock private OrderStatusHistoryRepository historyRepository;
    @Mock private CatalogClient catalogClient;
    @Mock private PaymentClient paymentClient;

    private OrderQueryService orderQueryService;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        orderQueryService = new OrderQueryService(
                orderRepository, orderItemRepository, shippingAddressRepository, historyRepository, catalogClient, paymentClient
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

    @Test
    void listOrders_mapsOrdersWithCoverImages() {
        Order order = newOrder(OrderStatus.CONFIRMED, DeliveryType.PHYSICAL);
        OrderItem item = new OrderItem(order, UUID.randomUUID(), "Title", "9780000000001", BigDecimal.TEN, 1, DeliveryType.PHYSICAL);
        when(orderRepository.findAllByUserIdOrderByPlacedAtDesc(any(), any())).thenReturn(new PageImpl<>(List.of(order)));
        when(orderItemRepository.findAllByOrderIdIn(any())).thenReturn(List.of(item));
        when(catalogClient.getCoverImageUrls(any())).thenReturn(Map.of(item.getBookId(), "http://cover.jpg"));

        var page = orderQueryService.listOrders(userId, Pageable.unpaged());

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).itemPreviews()).hasSize(1);
    }

    @Test
    void getDetail_notFound_throws() {
        when(orderRepository.findByIdAndUserId(any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderQueryService.getDetail(userId, UUID.randomUUID()))
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

        var detail = orderQueryService.getDetail(userId, order.getId());

        assertThat(detail.payment()).isNull();
    }

    @Test
    void getDetail_found_mapsItemsAddressAndPayment() {
        Order order = newOrder(OrderStatus.CONFIRMED, DeliveryType.PHYSICAL);
        OrderItem item = new OrderItem(order, UUID.randomUUID(), "Title", "9780000000001", BigDecimal.TEN, 1, DeliveryType.PHYSICAL);
        OrderShippingAddress address = new OrderShippingAddress(
                order, "Name", "Line 1", null, "Mumbai", "MH", "400001", "IN", "999");
        when(orderRepository.findByIdAndUserId(order.getId(), userId)).thenReturn(Optional.of(order));
        when(orderItemRepository.findAllByOrderId(any())).thenReturn(List.of(item));
        when(shippingAddressRepository.findByOrderId(any())).thenReturn(Optional.of(address));
        when(historyRepository.findAllByOrderIdOrderByChangedAt(any())).thenReturn(List.of());
        when(paymentClient.getPaymentDetails(any())).thenReturn(Optional.of(
                new PaymentDetails(UUID.randomUUID(), order.getId(), "CAPTURED", "UPI",
                        order.getGrandTotal(), BigDecimal.ZERO, Instant.now(), Instant.now())));

        var detail = orderQueryService.getDetail(userId, order.getId());

        assertThat(detail.items()).hasSize(1);
        assertThat(detail.shippingAddress().city()).isEqualTo("Mumbai");
        assertThat(detail.payment().status()).isEqualTo("CAPTURED");
    }

    @Test
    void getDeliveryDetail_notFound_throws() {
        when(orderRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderQueryService.getDeliveryDetail(UUID.randomUUID())).isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void getDeliveryDetail_found_mapsItemsAndAddress() {
        Order order = newOrder(OrderStatus.CONFIRMED, DeliveryType.PHYSICAL);
        OrderItem item = new OrderItem(order, UUID.randomUUID(), "Title", "9780000000001", BigDecimal.TEN, 2, DeliveryType.PHYSICAL);
        OrderShippingAddress address = new OrderShippingAddress(
                order, "Name", "Line 1", "Line 2", "Mumbai", "MH", "400001", "IN", "999");
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(orderItemRepository.findAllByOrderId(any())).thenReturn(List.of(item));
        when(shippingAddressRepository.findByOrderId(any())).thenReturn(Optional.of(address));

        var detail = orderQueryService.getDeliveryDetail(order.getId());

        assertThat(detail.items()).hasSize(1);
        assertThat(detail.shippingAddress().city()).isEqualTo("Mumbai");
    }
}
