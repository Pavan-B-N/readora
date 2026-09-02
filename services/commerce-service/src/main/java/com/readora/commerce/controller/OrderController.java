package com.readora.commerce.controller;

import com.readora.commerce.dto.CancelOrderRequest;
import com.readora.commerce.dto.CancelOrderResponse;
import com.readora.commerce.dto.CheckoutRequest;
import com.readora.commerce.dto.CheckoutResponse;
import com.readora.commerce.dto.OrderDetailResponse;
import com.readora.commerce.dto.OrderSummaryResponse;
import com.readora.commerce.dto.PostReturnMessageRequest;
import com.readora.commerce.dto.ReturnMessageResponse;
import com.readora.commerce.dto.ReturnOrderRequest;
import com.readora.commerce.dto.ReturnOrderResponse;
import com.readora.sharedcore.security.CurrentUserContext;
import com.readora.commerce.service.CheckoutService;
import com.readora.commerce.service.OrderQueryService;
import com.readora.commerce.service.ReturnService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Orders")
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final CheckoutService checkoutService;
    private final OrderQueryService orderQueryService;
    private final ReturnService returnService;

    public OrderController(CheckoutService checkoutService, OrderQueryService orderQueryService, ReturnService returnService) {
        this.checkoutService = checkoutService;
        this.orderQueryService = orderQueryService;
        this.returnService = returnService;
    }

    @Operation(
            summary = "Check out",
            description = "Reserves stock for PHYSICAL items and confirms virtual-edition availability for VIRTUAL items — a checkout can mix both. Prices the order and creates it in PENDING_PAYMENT. Returns as soon as the order is durably recorded — payment settles asynchronously off Kafka. Send an Idempotency-Key header; a replay returns the original order rather than creating a duplicate.",
            tags = {"Orders"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Order created in PENDING_PAYMENT"),
            @ApiResponse(responseCode = "400", description = "shippingAddress is missing for a PHYSICAL order"),
            @ApiResponse(responseCode = "404", description = "One of the requested books does not exist or is inactive"),
            @ApiResponse(responseCode = "409", description = "The item list was empty, a title went out of stock (PHYSICAL), or a book has no virtual edition available (VIRTUAL)")
    })
    @PostMapping("/checkout")
    public ResponseEntity<CheckoutResponse> checkout(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CheckoutRequest request
    ) {
        CheckoutResponse response = checkoutService.checkout(CurrentUserContext.require(), idempotencyKey, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "List order history",
            description = "Paginated order history for the caller, newest first.",
            tags = {"Orders"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order history page returned")
    })
    @GetMapping
    public ResponseEntity<Page<OrderSummaryResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(orderQueryService.listOrders(CurrentUserContext.require(), pageable));
    }

    @Operation(
            summary = "Get order detail",
            description = "Returns full order detail including line items, shipping-address snapshot, and status history.",
            tags = {"Orders"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order detail returned"),
            @ApiResponse(responseCode = "404", description = "No such order, or it belongs to another user")
    })
    @GetMapping("/{id}")
    public ResponseEntity<OrderDetailResponse> getDetail(@PathVariable UUID id) {
        return ResponseEntity.ok(orderQueryService.getDetail(CurrentUserContext.require(), id));
    }

    @Operation(
            summary = "Cancel an order",
            description = "Cancels an order. Permitted only within 48 hours of placement and only while the order has not shipped. Triggers an asynchronous refund off Kafka.",
            tags = {"Orders"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order cancelled, refund pending"),
            @ApiResponse(responseCode = "404", description = "No such order, or it belongs to another user"),
            @ApiResponse(responseCode = "409", description = "The cancel window has expired, the order already shipped, or it's already cancelled")
    })
    @PostMapping("/{id}/cancel")
    public ResponseEntity<CancelOrderResponse> cancel(@PathVariable UUID id, @RequestBody CancelOrderRequest request) {
        return ResponseEntity.ok(returnService.cancel(CurrentUserContext.require(), id, request));
    }

    @Operation(
            summary = "Return a delivered order",
            description = "Returns an order. Permitted only once it's DELIVERED and within 2 days of delivery, and requires a reason. Triggers an asynchronous refund off Kafka, same as cancellation.",
            tags = {"Orders"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order returned, refund pending"),
            @ApiResponse(responseCode = "400", description = "Missing return reason"),
            @ApiResponse(responseCode = "404", description = "No such order, or it belongs to another user"),
            @ApiResponse(responseCode = "409", description = "The order isn't delivered yet, or the return window has expired")
    })
    @PostMapping("/{id}/return")
    public ResponseEntity<ReturnOrderResponse> returnOrder(@PathVariable UUID id, @Valid @RequestBody ReturnOrderRequest request) {
        return ResponseEntity.ok(returnService.returnOrder(CurrentUserContext.require(), id, request));
    }

    @Operation(
            summary = "Get the return chat for an order",
            description = "The small back-and-forth between the customer and an admin while a return sits at RETURN_REQUESTED. Always readable, even after the return is decided.",
            tags = {"Orders"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Messages returned (possibly empty)"),
            @ApiResponse(responseCode = "404", description = "No such order, or it belongs to another user")
    })
    @GetMapping("/{id}/return/messages")
    public ResponseEntity<List<ReturnMessageResponse>> returnMessages(@PathVariable UUID id) {
        return ResponseEntity.ok(returnService.listReturnMessages(CurrentUserContext.require(), id));
    }

    @Operation(
            summary = "Send a return chat message",
            description = "Only accepted while the return is RETURN_REQUESTED — locked once an admin approves or rejects it.",
            tags = {"Orders"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Message sent"),
            @ApiResponse(responseCode = "404", description = "No such order, or it belongs to another user"),
            @ApiResponse(responseCode = "409", description = "The return isn't awaiting review anymore")
    })
    @PostMapping("/{id}/return/messages")
    public ResponseEntity<ReturnMessageResponse> postReturnMessage(@PathVariable UUID id, @Valid @RequestBody PostReturnMessageRequest request) {
        return ResponseEntity.ok(returnService.postReturnMessage(CurrentUserContext.require(), id, request.content()));
    }
}
