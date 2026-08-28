package com.readora.commerce.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.readora.commerce.cart.CartRepository;
import com.readora.commerce.client.CatalogClient;
import com.readora.commerce.client.PaymentClient;
import com.readora.commerce.client.UserServiceClient;
import com.readora.commerce.dto.CancelOrderRequest;
import com.readora.commerce.dto.CancelOrderResponse;
import com.readora.commerce.dto.CheckoutRequest;
import com.readora.commerce.dto.CheckoutResponse;
import com.readora.commerce.dto.OrderCancelledEvent;
import com.readora.commerce.dto.OrderCreatedEvent;
import com.readora.commerce.dto.OrderDeliveryDetailResponse;
import com.readora.commerce.dto.OrderDetailResponse;
import com.readora.commerce.dto.OrderReturnedEvent;
import com.readora.commerce.dto.OrderStatusChangedEvent;
import com.readora.commerce.dto.OrderSummaryResponse;
import com.readora.commerce.dto.ReserveStockRequest;
import com.readora.commerce.dto.ReserveStockResponse;
import com.readora.commerce.dto.ReturnMessageResponse;
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
import com.readora.commerce.entity.ReturnSenderRole;
import com.readora.commerce.exception.CartEmptyException;
import com.readora.commerce.exception.InsufficientWalletBalanceException;
import com.readora.commerce.exception.InvalidDeliveryTransitionException;
import com.readora.commerce.exception.InvalidPaymentMethodException;
import com.readora.commerce.exception.InvalidReturnTransitionException;
import com.readora.commerce.exception.MultipleStoresInCartException;
import com.readora.commerce.exception.OrderAlreadyCancelledException;
import com.readora.commerce.exception.OrderAlreadyShippedException;
import com.readora.commerce.exception.OrderCancelWindowExpiredException;
import com.readora.commerce.exception.OrderNotFoundException;
import com.readora.commerce.exception.OrderNotReturnableException;
import com.readora.commerce.exception.ReturnNotUnderReviewException;
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
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final BigDecimal TAX_RATE = new BigDecimal("0.09");
    private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("499.00");
    private static final BigDecimal FLAT_SHIPPING_FEE = new BigDecimal("40.00");
    private static final BigDecimal PACKAGING_FEE = new BigDecimal("15.00");
    private static final Set<String> SUPPORTED_PAYMENT_METHODS = Set.of("WALLET", "UPI");
    /** How many line items the order list's cover collage shows before collapsing into "+N more". */
    private static final int ORDER_LIST_ITEM_PREVIEW_LIMIT = 4;

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderShippingAddressRepository shippingAddressRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final CatalogClient catalogClient;
    private final UserServiceClient userServiceClient;
    private final PaymentClient paymentClient;
    private final CartRepository cartRepository;
    private final ObjectMapper objectMapper;
    private final ReturnMessageService returnMessageService;

    public OrderService(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            OrderShippingAddressRepository shippingAddressRepository,
            OrderStatusHistoryRepository historyRepository,
            OutboxEventRepository outboxEventRepository,
            CatalogClient catalogClient,
            UserServiceClient userServiceClient,
            PaymentClient paymentClient,
            CartRepository cartRepository,
            ObjectMapper objectMapper,
            ReturnMessageService returnMessageService
    ) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.shippingAddressRepository = shippingAddressRepository;
        this.historyRepository = historyRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.catalogClient = catalogClient;
        this.userServiceClient = userServiceClient;
        this.paymentClient = paymentClient;
        this.cartRepository = cartRepository;
        this.objectMapper = objectMapper;
        this.returnMessageService = returnMessageService;
    }

    /** One resolved, priced line — physical (reserved stock) or virtual (looked-up edition) — before persistence. */
    private record PricedLine(
            UUID bookId, String title, String isbnSnapshot, BigDecimal unitPrice, int qty,
            DeliveryType deliveryType, UUID storeId
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
        if (!SUPPORTED_PAYMENT_METHODS.contains(paymentMethod)) {
            throw new InvalidPaymentMethodException("Unsupported payment method: " + request.paymentMethod());
        }

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
        order.setStoreId(resolveStoreId(lines));
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
            lines.add(new PricedLine(
                    item.bookId(), item.title(), item.isbn13(), item.unitPrice(), qty, DeliveryType.PHYSICAL, item.storeId()
            ));
        }
        return lines;
    }

    /**
     * Physical browsing is store-scoped per customer, so every physical line should share one
     * store — this is a defensive check, not an expected path, given there's no legitimate way
     * today to add items from two different stores to the same cart.
     */
    private UUID resolveStoreId(List<PricedLine> lines) {
        List<UUID> storeIds = lines.stream()
                .filter(l -> l.deliveryType() == DeliveryType.PHYSICAL)
                .map(PricedLine::storeId)
                .distinct()
                .toList();
        if (storeIds.size() > 1) {
            throw new MultipleStoresInCartException();
        }
        return storeIds.isEmpty() ? null : storeIds.get(0);
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
            lines.add(new PricedLine(item.bookId(), item.title(), null, item.price(), qty, DeliveryType.VIRTUAL, null));
        }
        return lines;
    }

    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> listOrders(UUID userId, Pageable pageable) {
        Page<Order> orders = orderRepository.findAllByUserIdOrderByPlacedAtDesc(userId, pageable);
        List<UUID> orderIds = orders.getContent().stream().map(Order::getId).toList();

        Map<UUID, List<OrderItem>> itemsByOrderId = orderItemRepository.findAllByOrderIdIn(orderIds).stream()
                .collect(Collectors.groupingBy(item -> item.getOrder().getId()));

        List<UUID> distinctBookIds = itemsByOrderId.values().stream()
                .flatMap(List::stream)
                .map(OrderItem::getBookId)
                .distinct()
                .toList();
        Map<UUID, String> coverImageUrls = catalogClient.getCoverImageUrls(distinctBookIds);

        return orders.map(order -> {
            List<OrderItem> items = itemsByOrderId.getOrDefault(order.getId(), List.of());
            List<OrderSummaryResponse.ItemPreview> previews = items.stream()
                    .limit(ORDER_LIST_ITEM_PREVIEW_LIMIT)
                    .map(item -> new OrderSummaryResponse.ItemPreview(
                            item.getBookId(), item.getTitleSnapshot(), coverImageUrls.get(item.getBookId())
                    ))
                    .toList();

            return new OrderSummaryResponse(
                    order.getId(), order.getOrderNumber(), order.getStatus().name(), order.getGrandTotal(),
                    order.getCurrency(), order.getPlacedAt(), order.isCancellable(), order.getDeliveredAt(),
                    previews, items.size()
            );
        });
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

        OrderDetailResponse.PaymentInfo paymentInfo = paymentClient.getPaymentDetails(orderId)
                .map(p -> new OrderDetailResponse.PaymentInfo(
                        p.paymentId(), p.status(), p.amount(), p.walletAmountUsed(), p.authorizedAt(), p.capturedAt()
                ))
                .orElse(null);

        return new OrderDetailResponse(
                order.getId(), order.getOrderNumber(), order.getStatus().name(), order.getDeliveryType().name(),
                items, addressDto, history, order.isCancellable(), order.isReturnable(), order.getSubtotal(), order.getShippingFee(),
                order.getPackagingFee(), order.getTaxAmount(), order.getGrandTotal(), order.getWalletAmountUsed(),
                order.getPaymentMethod(), order.getCurrency(), order.getPlacedAt(),
                order.getDeliveryAgentName(), order.getDeliveredAt(), paymentInfo, order.getReturnAgentName()
        );
    }

    @Transactional
    public CancelOrderResponse cancel(UUID userId, UUID orderId, CancelOrderRequest request) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId).orElseThrow(OrderNotFoundException::new);

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new OrderAlreadyCancelledException();
        }
        if (order.getStatus() == OrderStatus.ASSIGNED
                || order.getStatus() == OrderStatus.SHIPPED
                || order.getStatus() == OrderStatus.DELIVERED) {
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

    /**
     * A virtual-only order has nothing physical to inspect, so it auto-advances straight through
     * to REFUND_INITIATED in this same call — no admin review, no chat. An order with a physical
     * item stops at RETURN_REQUESTED and waits for an admin decision (see reviewReturn()); the
     * refund itself doesn't fire until the book is actually collected
     * (updateReturnPickupStatus() -> RETURN_COLLECTED).
     */
    @Transactional
    public ReturnOrderResponse returnOrder(UUID userId, UUID orderId, ReturnOrderRequest request) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId).orElseThrow(OrderNotFoundException::new);

        if (!order.isReturnable()) {
            throw new OrderNotReturnableException();
        }

        OrderStatus previousStatus = order.getStatus();
        order.requestReturn(request.reason());
        orderRepository.save(order);
        recordHistory(order, previousStatus, OrderStatus.RETURN_REQUESTED, request.reason(), "user");

        if (order.getDeliveryType() == DeliveryType.VIRTUAL) {
            initiateRefund(order, request.reason(), "system");
        }

        return new ReturnOrderResponse(order.getId(), order.getStatus().name(), order.getCancelledAt());
    }

    /**
     * The admin decision gate for a return with a physical item (a virtual-only return never
     * reaches RETURN_REQUESTED for long enough to be reviewed — see returnOrder()). Approving
     * queues a pickup for delivery-agent-service (it listens for RETURN_APPROVED on
     * order.status_changed); rejecting is terminal, no refund.
     */
    @Transactional
    public void reviewReturn(UUID adminId, UUID orderId, UUID storeId, String decision, String note) {
        Order order = orderRepository.findByIdAndStoreId(orderId, storeId).orElseThrow(OrderNotFoundException::new);
        if (order.getStatus() != OrderStatus.RETURN_REQUESTED) {
            throw new ReturnNotUnderReviewException();
        }

        order.markReviewed(adminId, note);

        OrderStatus previousStatus = order.getStatus();
        if ("APPROVE".equalsIgnoreCase(decision)) {
            order.approveReturn();
            orderRepository.save(order);
            recordHistory(order, previousStatus, OrderStatus.RETURN_APPROVED, note, "admin");
        } else if ("REJECT".equalsIgnoreCase(decision)) {
            order.rejectReturn();
            orderRepository.save(order);
            recordHistory(order, previousStatus, OrderStatus.RETURN_REJECTED, note, "admin");
        } else {
            throw new IllegalArgumentException("decision must be APPROVE or REJECT");
        }
    }

    /**
     * Called by delivery-agent-service (via InternalDeliveryController) as an agent progresses a
     * return pickup. Enforces RETURN_APPROVED -> RETURN_ASSIGNED -> RETURN_EN_ROUTE ->
     * RETURN_COLLECTED in order. Reaching RETURN_COLLECTED immediately kicks off the refund —
     * mirrors updateDeliveryStatus()'s shape exactly, just for the reverse (pickup) leg.
     */
    @Transactional
    public void updateReturnPickupStatus(UUID orderId, OrderStatus newStatus, UUID returnAgentId, String returnAgentName) {
        Order order = orderRepository.findById(orderId).orElseThrow(OrderNotFoundException::new);
        OrderStatus previousStatus = order.getStatus();

        boolean legal = switch (newStatus) {
            case RETURN_ASSIGNED -> previousStatus == OrderStatus.RETURN_APPROVED;
            case RETURN_EN_ROUTE -> previousStatus == OrderStatus.RETURN_ASSIGNED;
            case RETURN_COLLECTED -> previousStatus == OrderStatus.RETURN_EN_ROUTE;
            default -> false;
        };
        if (!legal) {
            throw new InvalidReturnTransitionException();
        }

        switch (newStatus) {
            case RETURN_ASSIGNED -> order.assignReturnAgent(returnAgentId, returnAgentName);
            case RETURN_EN_ROUTE -> order.markReturnEnRoute();
            case RETURN_COLLECTED -> order.markReturnCollected();
            default -> throw new InvalidReturnTransitionException();
        }
        orderRepository.save(order);
        recordHistory(order, previousStatus, newStatus, null, "delivery-agent");

        if (newStatus == OrderStatus.RETURN_COLLECTED) {
            initiateRefund(order, order.getCancelReason(), "system");
        }
    }

    /** Payment-service confirmed the refund actually landed — the one true terminal hop for every return path. */
    @Transactional
    public void handleRefundCompleted(UUID orderId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null || order.getStatus() != OrderStatus.REFUND_INITIATED) {
            return;
        }

        OrderStatus previousStatus = order.getStatus();
        order.completeReturn();
        orderRepository.save(order);
        recordHistory(order, previousStatus, OrderStatus.RETURNED, null, "system");
    }

    /** Shared by both return paths: fires REFUND_INITIATED plus the order.returned event payment-service already consumes unchanged. */
    private void initiateRefund(Order order, String reason, String changedBy) {
        OrderStatus previousStatus = order.getStatus();
        order.initiateRefund();
        orderRepository.save(order);
        recordHistory(order, previousStatus, OrderStatus.REFUND_INITIATED, reason, changedBy);

        publish("Order", order.getId(), KafkaTopics.ORDER_RETURNED,
                new OrderReturnedEvent(order.getId(), order.getUserId(), reason, order.getGrandTotal()));
    }

    @Transactional(readOnly = true)
    public List<ReturnMessageResponse> listReturnMessages(UUID userId, UUID orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId).orElseThrow(OrderNotFoundException::new);
        return returnMessageService.list(order.getId());
    }

    @Transactional
    public ReturnMessageResponse postReturnMessage(UUID userId, UUID orderId, String content) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId).orElseThrow(OrderNotFoundException::new);
        return returnMessageService.post(order, userId, ReturnSenderRole.CUSTOMER, content);
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
            order.markDelivered();
            orderRepository.save(order);
            recordHistory(order, OrderStatus.CONFIRMED, OrderStatus.DELIVERED, null, "system");
        }
    }

    /** Full order detail for delivery-agent-service — see OrderDeliveryDetailResponse's javadoc for why this isn't OrderDetailResponse. */
    @Transactional(readOnly = true)
    public OrderDeliveryDetailResponse getDeliveryDetail(UUID orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow(OrderNotFoundException::new);

        List<OrderDeliveryDetailResponse.Item> items = orderItemRepository.findAllByOrderId(orderId).stream()
                .map(i -> new OrderDeliveryDetailResponse.Item(i.getTitleSnapshot(), i.getQty()))
                .toList();

        OrderShippingAddress address = shippingAddressRepository.findByOrderId(orderId).orElse(null);
        OrderDeliveryDetailResponse.ShippingAddress addressDto = address != null
                ? new OrderDeliveryDetailResponse.ShippingAddress(
                        address.getRecipientName(), address.getLine1(), address.getLine2(), address.getCity(),
                        address.getState(), address.getPostalCode(), address.getCountryCode(), address.getPhone()
                )
                : null;

        return new OrderDeliveryDetailResponse(
                order.getId(), order.getOrderNumber(), order.getStatus().name(), order.getStoreId(),
                addressDto, items, order.getPlacedAt()
        );
    }

    /**
     * Called by delivery-agent-service (via InternalDeliveryController) as an agent progresses a
     * physical order. Enforces CONFIRMED -> ASSIGNED -> SHIPPED -> DELIVERED in order — no
     * skipping a step.
     */
    @Transactional
    public void updateDeliveryStatus(UUID orderId, OrderStatus newStatus, UUID deliveryAgentId, String deliveryAgentName) {
        Order order = orderRepository.findById(orderId).orElseThrow(OrderNotFoundException::new);
        OrderStatus previousStatus = order.getStatus();

        boolean legal = switch (newStatus) {
            case ASSIGNED -> previousStatus == OrderStatus.CONFIRMED;
            case SHIPPED -> previousStatus == OrderStatus.ASSIGNED;
            case DELIVERED -> previousStatus == OrderStatus.SHIPPED;
            default -> false;
        };
        if (!legal) {
            throw new InvalidDeliveryTransitionException();
        }

        switch (newStatus) {
            case ASSIGNED -> order.assignToAgent(deliveryAgentId, deliveryAgentName);
            case SHIPPED -> order.markOutForDelivery();
            case DELIVERED -> order.markDelivered();
            default -> throw new InvalidDeliveryTransitionException();
        }
        orderRepository.save(order);
        recordHistory(order, previousStatus, newStatus, null, "delivery-agent");
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
                order.getId(), order.getUserId(), order.getOrderNumber(), toStatus.name(),
                order.getDeliveryType().name(), order.getStoreId()
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
