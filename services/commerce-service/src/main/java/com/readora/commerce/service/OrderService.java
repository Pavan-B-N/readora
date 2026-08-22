package com.readora.commerce.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.readora.commerce.client.CatalogClient;
import com.readora.commerce.dto.CancelOrderRequest;
import com.readora.commerce.dto.CancelOrderResponse;
import com.readora.commerce.dto.CheckoutRequest;
import com.readora.commerce.dto.CheckoutResponse;
import com.readora.commerce.dto.OrderCancelledEvent;
import com.readora.commerce.dto.OrderCreatedEvent;
import com.readora.commerce.dto.OrderDetailResponse;
import com.readora.commerce.dto.OrderSummaryResponse;
import com.readora.commerce.dto.ReserveStockRequest;
import com.readora.commerce.dto.ReserveStockResponse;
import com.readora.commerce.entity.Order;
import com.readora.commerce.entity.OrderItem;
import com.readora.commerce.entity.OrderShippingAddress;
import com.readora.commerce.entity.OrderStatus;
import com.readora.commerce.entity.OrderStatusHistory;
import com.readora.commerce.entity.OutboxEvent;
import com.readora.commerce.exception.CartEmptyException;
import com.readora.commerce.exception.OrderAlreadyCancelledException;
import com.readora.commerce.exception.OrderAlreadyShippedException;
import com.readora.commerce.exception.OrderCancelWindowExpiredException;
import com.readora.commerce.exception.OrderNotFoundException;
import com.readora.commerce.kafka.KafkaTopics;
import com.readora.commerce.repository.OrderItemRepository;
import com.readora.commerce.repository.OrderRepository;
import com.readora.commerce.repository.OrderShippingAddressRepository;
import com.readora.commerce.repository.OrderStatusHistoryRepository;
import com.readora.commerce.repository.OutboxEventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class OrderService {

    private static final BigDecimal TAX_RATE = new BigDecimal("0.09");

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderShippingAddressRepository shippingAddressRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final CatalogClient catalogClient;
    private final ObjectMapper objectMapper;

    public OrderService(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            OrderShippingAddressRepository shippingAddressRepository,
            OrderStatusHistoryRepository historyRepository,
            OutboxEventRepository outboxEventRepository,
            CatalogClient catalogClient,
            ObjectMapper objectMapper
    ) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.shippingAddressRepository = shippingAddressRepository;
        this.historyRepository = historyRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.catalogClient = catalogClient;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public CheckoutResponse checkout(UUID userId, String idempotencyKey, CheckoutRequest request) {
        if (request.items().isEmpty()) {
            throw new CartEmptyException();
        }

        Order existing = orderRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (existing != null) {
            return toCheckoutResponse(existing);
        }

        ReserveStockRequest reserveRequest = new ReserveStockRequest(
                request.items().stream()
                        .map(i -> new ReserveStockRequest.Item(i.bookId(), i.qty()))
                        .toList()
        );
        ReserveStockResponse reserved = catalogClient.reserveStock(reserveRequest);

        BigDecimal subtotal = BigDecimal.ZERO;
        for (int idx = 0; idx < reserved.items().size(); idx++) {
            ReserveStockResponse.Item item = reserved.items().get(idx);
            int qty = request.items().get(idx).qty();
            subtotal = subtotal.add(item.unitPrice().multiply(BigDecimal.valueOf(qty)));
        }

        BigDecimal shippingFee = BigDecimal.ZERO;
        BigDecimal taxAmount = subtotal.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal grandTotal = subtotal.add(shippingFee).add(taxAmount);

        Order order = new Order(
                generateOrderNumber(), userId, "INR", subtotal, shippingFee, taxAmount, grandTotal, idempotencyKey
        );
        orderRepository.save(order);

        for (int idx = 0; idx < reserved.items().size(); idx++) {
            ReserveStockResponse.Item item = reserved.items().get(idx);
            int qty = request.items().get(idx).qty();
            orderItemRepository.save(new OrderItem(order, item.bookId(), item.title(), item.isbn13(), item.unitPrice(), qty));
        }

        CheckoutRequest.ShippingAddress addr = request.shippingAddress();
        shippingAddressRepository.save(new OrderShippingAddress(
                order, addr.recipientName(), addr.line1(), addr.line2(), addr.city(),
                addr.state(), addr.postalCode(), addr.countryCode(), addr.phone()
        ));

        historyRepository.save(new OrderStatusHistory(order, null, OrderStatus.PENDING_PAYMENT, null, "system"));

        publishOrderCreated(order, request);

        return toCheckoutResponse(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> listOrders(UUID userId, Pageable pageable) {
        return orderRepository.findAllByUserIdOrderByPlacedAtDesc(userId, pageable)
                .map(order -> new OrderSummaryResponse(
                        order.getId(), order.getOrderNumber(), order.getStatus().name(), order.getGrandTotal(),
                        order.getCurrency(), order.getPlacedAt(), order.isCancellable()
                ));
    }

    @Transactional(readOnly = true)
    public OrderDetailResponse getDetail(UUID userId, UUID orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId).orElseThrow(OrderNotFoundException::new);

        List<OrderDetailResponse.Item> items = orderItemRepository.findAllByOrderId(orderId).stream()
                .map(i -> new OrderDetailResponse.Item(
                        i.getBookId(), i.getTitleSnapshot(), i.getIsbnSnapshot(), i.getQty(),
                        i.getUnitPriceSnapshot(), i.getLineTotal()
                ))
                .toList();

        OrderShippingAddress address = shippingAddressRepository.findByOrderId(orderId).orElse(null);
        OrderDetailResponse.ShippingAddress addressDto = address != null
                ? new OrderDetailResponse.ShippingAddress(
                        address.getRecipientName(), address.getLine1(), address.getCity(),
                        address.getPostalCode(), address.getCountryCode()
                )
                : null;

        List<OrderDetailResponse.HistoryEntry> history = historyRepository.findAllByOrderIdOrderByChangedAt(orderId).stream()
                .map(h -> new OrderDetailResponse.HistoryEntry(h.getToStatus().name(), h.getChangedAt()))
                .toList();

        return new OrderDetailResponse(
                order.getId(), order.getOrderNumber(), order.getStatus().name(), items, addressDto, history,
                order.isCancellable(), order.getGrandTotal(), order.getCurrency(), order.getPlacedAt()
        );
    }

    @Transactional
    public CancelOrderResponse cancel(UUID userId, UUID orderId, CancelOrderRequest request) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId).orElseThrow(OrderNotFoundException::new);

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new OrderAlreadyCancelledException();
        }
        if (order.getStatus() == OrderStatus.SHIPPED || order.getStatus() == OrderStatus.DELIVERED) {
            throw new OrderAlreadyShippedException();
        }
        if (!order.isCancellable()) {
            throw new OrderCancelWindowExpiredException();
        }

        OrderStatus previousStatus = order.getStatus();
        order.cancel(request.reason());
        orderRepository.save(order);

        historyRepository.save(new OrderStatusHistory(order, previousStatus, OrderStatus.CANCELLED, request.reason(), "user"));

        publish("Order", order.getId(), KafkaTopics.ORDER_CANCELLED,
                new OrderCancelledEvent(order.getId(), userId, request.reason(), order.getGrandTotal()));

        return new CancelOrderResponse(order.getId(), order.getStatus().name(), order.getCancelledAt());
    }

    @Transactional
    public void handlePaymentCaptured(UUID orderId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null || order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            return;
        }

        order.transitionTo(OrderStatus.PAID);
        orderRepository.save(order);
        historyRepository.save(new OrderStatusHistory(order, OrderStatus.PENDING_PAYMENT, OrderStatus.PAID, null, "system"));

        order.transitionTo(OrderStatus.CONFIRMED);
        orderRepository.save(order);
        historyRepository.save(new OrderStatusHistory(order, OrderStatus.PAID, OrderStatus.CONFIRMED, null, "system"));
    }

    @Transactional
    public void handlePaymentFailed(UUID orderId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null || order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            return;
        }

        order.transitionTo(OrderStatus.PAYMENT_FAILED);
        orderRepository.save(order);
        historyRepository.save(new OrderStatusHistory(order, OrderStatus.PENDING_PAYMENT, OrderStatus.PAYMENT_FAILED, null, "system"));
    }

    private void publishOrderCreated(Order order, CheckoutRequest request) {
        List<OrderCreatedEvent.Item> items = request.items().stream()
                .map(i -> new OrderCreatedEvent.Item(i.bookId(), i.qty()))
                .toList();

        publish("Order", order.getId(), KafkaTopics.ORDER_CREATED, new OrderCreatedEvent(
                order.getId(), order.getUserId(), items, order.getGrandTotal(), BigDecimal.ZERO, request.paymentMethod()
        ));
    }

    private void publish(String aggregateType, UUID aggregateId, String topic, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            outboxEventRepository.save(new OutboxEvent(aggregateType, aggregateId, topic, json));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize outbox event payload", e);
        }
    }

    private CheckoutResponse toCheckoutResponse(Order order) {
        return new CheckoutResponse(
                order.getId(), order.getOrderNumber(), order.getStatus().name(), order.getSubtotal(),
                order.getShippingFee(), order.getTaxAmount(), order.getGrandTotal(), order.getCurrency(), order.getPlacedAt()
        );
    }

    private String generateOrderNumber() {
        int year = Instant.now().atZone(java.time.ZoneOffset.UTC).getYear();
        int random = ThreadLocalRandom.current().nextInt(1_000_000);
        return "RDA-%d-%06d".formatted(year, random);
    }
}
