package com.readora.commerce.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.readora.commerce.cart.CartRepository;
import com.readora.commerce.client.CatalogClient;
import com.readora.commerce.client.UserServiceClient;
import com.readora.commerce.dto.CancelOrderRequest;
import com.readora.commerce.dto.CancelOrderResponse;
import com.readora.commerce.dto.CheckoutRequest;
import com.readora.commerce.dto.CheckoutResponse;
import com.readora.commerce.dto.OrderCancelledEvent;
import com.readora.commerce.dto.OrderCreatedEvent;
import com.readora.commerce.dto.OrderDetailResponse;
import com.readora.commerce.dto.OrderReturnedEvent;
import com.readora.commerce.dto.OrderStatusChangedEvent;
import com.readora.commerce.dto.OrderSummaryResponse;
import com.readora.commerce.dto.ReserveStockRequest;
import com.readora.commerce.dto.ReserveStockResponse;
import com.readora.commerce.dto.ReturnOrderRequest;
import com.readora.commerce.dto.ReturnOrderResponse;
import com.readora.commerce.dto.VirtualEditionLookupRequest;
import com.readora.commerce.dto.VirtualEditionLookupResponse;
import com.readora.commerce.dto.WalletBalance;
import com.readora.commerce.entity.DeliveryType;
import com.readora.commerce.entity.Order;
import com.readora.commerce.entity.OrderItem;
import com.readora.commerce.entity.OrderShippingAddress;
import com.readora.commerce.entity.OrderStatus;
import com.readora.commerce.entity.OrderStatusHistory;
import com.readora.commerce.entity.OutboxEvent;
import com.readora.commerce.exception.CartEmptyException;
import com.readora.commerce.exception.InsufficientWalletBalanceException;
import com.readora.commerce.exception.OrderAlreadyCancelledException;
import com.readora.commerce.exception.OrderAlreadyShippedException;
import com.readora.commerce.exception.OrderCancelWindowExpiredException;
import com.readora.commerce.exception.OrderNotFoundException;
import com.readora.commerce.exception.OrderNotReturnableException;
import com.readora.commerce.exception.ShippingAddressRequiredException;
import com.readora.commerce.exception.VirtualEditionNotAvailableException;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class OrderService {

    private static final BigDecimal TAX_RATE = new BigDecimal("0.09");
    private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("499.00");
    private static final BigDecimal FLAT_SHIPPING_FEE = new BigDecimal("40.00");
    private static final BigDecimal PACKAGING_FEE = new BigDecimal("15.00");

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderShippingAddressRepository shippingAddressRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final CatalogClient catalogClient;
    private final UserServiceClient userServiceClient;
    private final CartRepository cartRepository;
    private final ObjectMapper objectMapper;

    public OrderService(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            OrderShippingAddressRepository shippingAddressRepository,
            OrderStatusHistoryRepository historyRepository,
            OutboxEventRepository outboxEventRepository,
            CatalogClient catalogClient,
            UserServiceClient userServiceClient,
            CartRepository cartRepository,
            ObjectMapper objectMapper
    ) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.shippingAddressRepository = shippingAddressRepository;
        this.historyRepository = historyRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.catalogClient = catalogClient;
        this.userServiceClient = userServiceClient;
        this.cartRepository = cartRepository;
        this.objectMapper = objectMapper;
    }

    /** One resolved, priced line — physical (reserved stock) or virtual (looked-up edition) — before persistence. */
    private record PricedLine(
            UUID bookId, String title, String isbnSnapshot, BigDecimal unitPrice, int qty, DeliveryType deliveryType
    ) {
    }

    @Transactional
    public CheckoutResponse checkout(UUID userId, String idempotencyKey, CheckoutRequest request) {
        if (request.items().isEmpty()) {
            throw new CartEmptyException();
        }

        List<CheckoutRequest.Item> physicalItems = request.items().stream()
                .filter(i -> i.deliveryType() == DeliveryType.PHYSICAL)
                .toList();
        List<CheckoutRequest.Item> virtualItems = request.items().stream()
                .filter(i -> i.deliveryType() == DeliveryType.VIRTUAL)
                .toList();

        boolean hasPhysical = !physicalItems.isEmpty();
        if (hasPhysical && request.shippingAddress() == null) {
            throw new ShippingAddressRequiredException();
        }

        Order existing = orderRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (existing != null) {
            return toCheckoutResponse(existing);
        }

        List<PricedLine> lines = new ArrayList<>();
        if (hasPhysical) {
            lines.addAll(reservePhysical(physicalItems));
        }
        if (!virtualItems.isEmpty()) {
            lines.addAll(lookupVirtual(virtualItems));
        }

        BigDecimal subtotal = lines.stream()
                .map(l -> l.unitPrice().multiply(BigDecimal.valueOf(l.qty())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal shippingFee = !hasPhysical
                ? BigDecimal.ZERO
                : subtotal.compareTo(FREE_SHIPPING_THRESHOLD) >= 0 ? BigDecimal.ZERO : FLAT_SHIPPING_FEE;
        BigDecimal packagingFee = hasPhysical ? PACKAGING_FEE : BigDecimal.ZERO;
        BigDecimal taxAmount = subtotal.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal grandTotal = subtotal.add(shippingFee).add(packagingFee).add(taxAmount);

        String paymentMethod = request.paymentMethod().toUpperCase();
        BigDecimal walletAmountUsed = BigDecimal.ZERO;
        if ("WALLET".equals(paymentMethod)) {
            WalletBalance balance = userServiceClient.getWalletBalance(userId);
            if (balance.balance().compareTo(grandTotal) < 0) {
                throw new InsufficientWalletBalanceException(grandTotal.subtract(balance.balance()), balance.currency());
            }
            walletAmountUsed = grandTotal;
        }

        DeliveryType orderDeliveryType = hasPhysical ? DeliveryType.PHYSICAL : DeliveryType.VIRTUAL;
        Order order = new Order(
                generateOrderNumber(), userId, "INR", subtotal, shippingFee, packagingFee, taxAmount, grandTotal,
                walletAmountUsed, paymentMethod, idempotencyKey, orderDeliveryType
        );
        orderRepository.save(order);

        for (PricedLine line : lines) {
            orderItemRepository.save(new OrderItem(
                    order, line.bookId(), line.title(), line.isbnSnapshot(), line.unitPrice(), line.qty(), line.deliveryType()
            ));
        }

        if (hasPhysical) {
            CheckoutRequest.ShippingAddress addr = request.shippingAddress();
            shippingAddressRepository.save(new OrderShippingAddress(
                    order, addr.recipientName(), addr.line1(), addr.line2(), addr.city(),
                    addr.state(), addr.postalCode(), addr.countryCode(), addr.phone()
            ));
        }

        recordHistory(order, null, OrderStatus.PENDING_PAYMENT, null, "system");
        publishOrderCreated(order, request, walletAmountUsed);
        cartRepository.clear(userId);

        return toCheckoutResponse(order);
    }

    /** Reserves physical stock and prices items at their live catalog list price. */
    private List<PricedLine> reservePhysical(List<CheckoutRequest.Item> physicalItems) {
        ReserveStockRequest reserveRequest = new ReserveStockRequest(
                physicalItems.stream().map(i -> new ReserveStockRequest.Item(i.bookId(), i.qty())).toList()
        );
        ReserveStockResponse reserved = catalogClient.reserveStock(reserveRequest);

        List<PricedLine> lines = new ArrayList<>();
        for (int idx = 0; idx < reserved.items().size(); idx++) {
            ReserveStockResponse.Item item = reserved.items().get(idx);
            int qty = physicalItems.get(idx).qty();
            lines.add(new PricedLine(item.bookId(), item.title(), item.isbn13(), item.unitPrice(), qty, DeliveryType.PHYSICAL));
        }
        return lines;
    }

    /**
     * No stock to reserve — a digital copy doesn't deplete. Every requested book must have an
     * active virtual edition, priced at the virtual edition's own price (which can differ from
     * the physical list price).
     */
    private List<PricedLine> lookupVirtual(List<CheckoutRequest.Item> virtualItems) {
        List<UUID> bookIds = virtualItems.stream().map(CheckoutRequest.Item::bookId).toList();
        VirtualEditionLookupResponse lookup = catalogClient.lookupVirtualEditions(new VirtualEditionLookupRequest(bookIds));

        List<PricedLine> lines = new ArrayList<>();
        for (int idx = 0; idx < lookup.items().size(); idx++) {
            VirtualEditionLookupResponse.Item item = lookup.items().get(idx);
            if (!item.available()) {
                throw new VirtualEditionNotAvailableException(item.bookId());
            }
            int qty = virtualItems.get(idx).qty();
            lines.add(new PricedLine(item.bookId(), item.title(), null, item.price(), qty, DeliveryType.VIRTUAL));
        }
        return lines;
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
                        i.getUnitPriceSnapshot(), i.getLineTotal(), i.getDeliveryType().name()
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
                order.getId(), order.getOrderNumber(), order.getStatus().name(), order.getDeliveryType().name(),
                items, addressDto, history, order.isCancellable(), order.isReturnable(), order.getSubtotal(), order.getShippingFee(),
                order.getPackagingFee(), order.getTaxAmount(), order.getGrandTotal(), order.getWalletAmountUsed(),
                order.getPaymentMethod(), order.getCurrency(), order.getPlacedAt()
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

        recordHistory(order, previousStatus, OrderStatus.CANCELLED, request.reason(), "user");

        publish("Order", order.getId(), KafkaTopics.ORDER_CANCELLED,
                new OrderCancelledEvent(order.getId(), userId, request.reason(), order.getGrandTotal()));

        return new CancelOrderResponse(order.getId(), order.getStatus().name(), order.getCancelledAt());
    }

    @Transactional
    public ReturnOrderResponse returnOrder(UUID userId, UUID orderId, ReturnOrderRequest request) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId).orElseThrow(OrderNotFoundException::new);

        if (!order.isReturnable()) {
            throw new OrderNotReturnableException();
        }

        OrderStatus previousStatus = order.getStatus();
        order.returnOrder(request.reason());
        orderRepository.save(order);

        recordHistory(order, previousStatus, OrderStatus.RETURNED, request.reason(), "user");

        publish("Order", order.getId(), KafkaTopics.ORDER_RETURNED,
                new OrderReturnedEvent(order.getId(), userId, request.reason(), order.getGrandTotal()));

        return new ReturnOrderResponse(order.getId(), order.getStatus().name(), order.getCancelledAt());
    }

    /**
     * Physical orders stop at CONFIRMED here — SHIPPED/DELIVERED come from a future shipping
     * integration, not built yet. Virtual orders have nothing to ship, so they go straight to
     * DELIVERED in the same step — "booking -> delivered" with no fulfillment lag.
     */
    @Transactional
    public void handlePaymentCaptured(UUID orderId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null || order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            return;
        }

        order.transitionTo(OrderStatus.PAID);
        orderRepository.save(order);
        recordHistory(order, OrderStatus.PENDING_PAYMENT, OrderStatus.PAID, null, "system");

        order.transitionTo(OrderStatus.CONFIRMED);
        orderRepository.save(order);
        recordHistory(order, OrderStatus.PAID, OrderStatus.CONFIRMED, null, "system");

        if (order.getDeliveryType() == DeliveryType.VIRTUAL) {
            order.transitionTo(OrderStatus.DELIVERED);
            orderRepository.save(order);
            recordHistory(order, OrderStatus.CONFIRMED, OrderStatus.DELIVERED, null, "system");
        }
    }

    @Transactional
    public void handlePaymentFailed(UUID orderId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null || order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            return;
        }

        order.transitionTo(OrderStatus.PAYMENT_FAILED);
        orderRepository.save(order);
        recordHistory(order, OrderStatus.PENDING_PAYMENT, OrderStatus.PAYMENT_FAILED, null, "system");
    }

    /** Records the transition and publishes order.status_changed — the single source powering the notification feed. */
    private void recordHistory(Order order, OrderStatus fromStatus, OrderStatus toStatus, String reason, String changedBy) {
        historyRepository.save(new OrderStatusHistory(order, fromStatus, toStatus, reason, changedBy));
        publish("Order", order.getId(), KafkaTopics.ORDER_STATUS_CHANGED, new OrderStatusChangedEvent(
                order.getId(), order.getUserId(), order.getOrderNumber(), toStatus.name()
        ));
    }

    private void publishOrderCreated(Order order, CheckoutRequest request, BigDecimal walletAmountUsed) {
        List<OrderCreatedEvent.Item> items = request.items().stream()
                .map(i -> new OrderCreatedEvent.Item(i.bookId(), i.qty()))
                .toList();

        publish("Order", order.getId(), KafkaTopics.ORDER_CREATED, new OrderCreatedEvent(
                order.getId(), order.getUserId(), items, order.getGrandTotal(), walletAmountUsed, order.getPaymentMethod()
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
                order.getId(), order.getOrderNumber(), order.getStatus().name(), order.getDeliveryType().name(),
                order.getSubtotal(), order.getShippingFee(), order.getPackagingFee(), order.getTaxAmount(),
                order.getGrandTotal(), order.getWalletAmountUsed(), order.getPaymentMethod(), order.getCurrency(),
                order.getPlacedAt()
        );
    }

    private String generateOrderNumber() {
        int year = Instant.now().atZone(java.time.ZoneOffset.UTC).getYear();
        int random = ThreadLocalRandom.current().nextInt(1_000_000);
        return "RDA-%d-%06d".formatted(year, random);
    }
}
