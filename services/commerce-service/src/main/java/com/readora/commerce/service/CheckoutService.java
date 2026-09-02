package com.readora.commerce.service;

import com.readora.commerce.cart.CartRepository;
import com.readora.commerce.client.CatalogClient;
import com.readora.commerce.client.UserServiceClient;
import com.readora.commerce.dto.CheckoutRequest;
import com.readora.commerce.dto.CheckoutResponse;
import com.readora.commerce.dto.OrderCreatedEvent;
import com.readora.commerce.dto.ReserveStockRequest;
import com.readora.commerce.dto.ReserveStockResponse;
import com.readora.commerce.dto.StoreInfo;
import com.readora.commerce.dto.VirtualEditionLookupRequest;
import com.readora.commerce.dto.VirtualEditionLookupResponse;
import com.readora.commerce.dto.WalletBalance;
import com.readora.commerce.entity.DeliveryType;
import com.readora.commerce.entity.Order;
import com.readora.commerce.entity.OrderItem;
import com.readora.commerce.entity.OrderShippingAddress;
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
import com.readora.commerce.kafka.KafkaTopics;
import com.readora.commerce.repository.OrderItemRepository;
import com.readora.commerce.repository.OrderRepository;
import com.readora.commerce.repository.OrderShippingAddressRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Prices and creates a new order — the one entry point onto the order lifecycle every other
 * order-lifecycle service (OrderQueryService, OrderFulfillmentService, ReturnService) picks up
 * from. Split out of the original monolithic OrderService: this is genuinely a different
 * responsibility from querying existing orders or progressing/reversing their lifecycle.
 */
@Service
public class CheckoutService {

    private static final BigDecimal TAX_RATE = new BigDecimal("0.09");
    private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("499.00");
    private static final BigDecimal FLAT_SHIPPING_FEE = new BigDecimal("40.00");
    private static final BigDecimal PACKAGING_FEE = new BigDecimal("15.00");

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderShippingAddressRepository shippingAddressRepository;
    private final CatalogClient catalogClient;
    private final UserServiceClient userServiceClient;
    private final CartRepository cartRepository;
    private final OrderEventRecorder eventRecorder;

    public CheckoutService(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            OrderShippingAddressRepository shippingAddressRepository,
            CatalogClient catalogClient,
            UserServiceClient userServiceClient,
            CartRepository cartRepository,
            OrderEventRecorder eventRecorder
    ) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.shippingAddressRepository = shippingAddressRepository;
        this.catalogClient = catalogClient;
        this.userServiceClient = userServiceClient;
        this.cartRepository = cartRepository;
        this.eventRecorder = eventRecorder;
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

        // Belt-and-suspenders on top of CartService's own check at add-to-cart time — this is the
        // actual point of no return (payment), so it's checked again here in case a caller ever
        // reaches checkout without having gone through the cart (e.g. a direct API call).
        for (CheckoutRequest.Item item : virtualItems) {
            if (orderItemRepository.existsActiveVirtualPurchase(userId, item.bookId())) {
                throw new VirtualEditionAlreadyOwnedException();
            }
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

        PaymentMethod paymentMethod = parsePaymentMethod(request.paymentMethod());

        BigDecimal walletAmountUsed = BigDecimal.ZERO;
        if (paymentMethod == PaymentMethod.WALLET) {
            WalletBalance balance = userServiceClient.getWalletBalance(userId);
            if (balance.balance().compareTo(grandTotal) < 0) {
                throw new InsufficientWalletBalanceException(grandTotal.subtract(balance.balance()), balance.currency());
            }
            walletAmountUsed = grandTotal;
        }

        UUID resolvedStoreId = resolveStoreId(lines);
        if (hasPhysical) {
            StoreInfo deliveringStore = catalogClient.getStore(resolvedStoreId);
            if (!deliveringStore.city().equalsIgnoreCase(request.shippingAddress().city())) {
                throw new ShippingAddressCityMismatchException(deliveringStore.city());
            }
        }

        DeliveryType orderDeliveryType = hasPhysical ? DeliveryType.PHYSICAL : DeliveryType.VIRTUAL;
        Order order = new Order(
                generateOrderNumber(), userId, "INR", subtotal, shippingFee, packagingFee, taxAmount, grandTotal,
                walletAmountUsed, paymentMethod, idempotencyKey, orderDeliveryType
        );
        order.setStoreId(resolvedStoreId);
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

        eventRecorder.recordHistory(order, null, OrderStatus.PENDING_PAYMENT, null, "system");
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

    private void publishOrderCreated(Order order, CheckoutRequest request, BigDecimal walletAmountUsed) {
        List<OrderCreatedEvent.Item> items = request.items().stream()
                .map(i -> new OrderCreatedEvent.Item(i.bookId(), i.qty()))
                .toList();

        eventRecorder.publish("Order", order.getId(), KafkaTopics.ORDER_CREATED, new OrderCreatedEvent(
                order.getId(), order.getUserId(), items, order.getGrandTotal(), walletAmountUsed, order.getPaymentMethod().name()
        ));
    }

    private CheckoutResponse toCheckoutResponse(Order order) {
        return new CheckoutResponse(
                order.getId(), order.getOrderNumber(), order.getStatus().name(), order.getDeliveryType().name(),
                order.getSubtotal(), order.getShippingFee(), order.getPackagingFee(), order.getTaxAmount(),
                order.getGrandTotal(), order.getWalletAmountUsed(), order.getPaymentMethod().name(), order.getCurrency(),
                order.getPlacedAt()
        );
    }

    private PaymentMethod parsePaymentMethod(String raw) {
        try {
            return PaymentMethod.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidPaymentMethodException("Unsupported payment method: " + raw);
        }
    }

    private String generateOrderNumber() {
        int year = Instant.now().atZone(java.time.ZoneOffset.UTC).getYear();
        int random = ThreadLocalRandom.current().nextInt(1_000_000);
        return "RDA-%d-%06d".formatted(year, random);
    }
}
