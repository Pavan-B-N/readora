package com.readora.commerce.service;

import com.readora.commerce.client.CatalogClient;
import com.readora.commerce.client.PaymentClient;
import com.readora.commerce.dto.OrderDeliveryDetailResponse;
import com.readora.commerce.dto.OrderDetailResponse;
import com.readora.commerce.dto.OrderSummaryResponse;
import com.readora.commerce.entity.Order;
import com.readora.commerce.entity.OrderItem;
import com.readora.commerce.entity.OrderShippingAddress;
import com.readora.commerce.exception.OrderNotFoundException;
import com.readora.commerce.repository.OrderItemRepository;
import com.readora.commerce.repository.OrderRepository;
import com.readora.commerce.repository.OrderShippingAddressRepository;
import com.readora.commerce.repository.OrderStatusHistoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Read-only order lookups — the caller's own history/detail (OrderController) and the internal
 * delivery-detail view (InternalDeliveryController, delivery-agent-service). Split out of the
 * original monolithic OrderService: a pure query surface, no state transitions, no side effects.
 */
@Service
public class OrderQueryService {

    /** How many line items the order list's cover collage shows before collapsing into "+N more". */
    private static final int ORDER_LIST_ITEM_PREVIEW_LIMIT = 4;

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderShippingAddressRepository shippingAddressRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final CatalogClient catalogClient;
    private final PaymentClient paymentClient;

    public OrderQueryService(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            OrderShippingAddressRepository shippingAddressRepository,
            OrderStatusHistoryRepository historyRepository,
            CatalogClient catalogClient,
            PaymentClient paymentClient
    ) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.shippingAddressRepository = shippingAddressRepository;
        this.historyRepository = historyRepository;
        this.catalogClient = catalogClient;
        this.paymentClient = paymentClient;
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
                order.getPaymentMethod().name(), order.getCurrency(), order.getPlacedAt(),
                order.getDeliveryAgentName(), order.getDeliveredAt(), paymentInfo, order.getReturnAgentName()
        );
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
                addressDto, items, order.getGrandTotal(), order.getPlacedAt()
        );
    }
}
