package com.readora.commerce.controller;

import com.readora.commerce.dto.PurchasedBookIdsResponse;
import com.readora.commerce.dto.RecentOrderItemResponse;
import com.readora.commerce.repository.OrderItemRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Internal")
@RestController
@RequestMapping("/internal/orders")
public class InternalOrderController {

    private final OrderItemRepository orderItemRepository;

    public InternalOrderController(OrderItemRepository orderItemRepository) {
        this.orderItemRepository = orderItemRepository;
    }

    @Operation(
            summary = "Get the distinct book ids a user has ordered",
            description = "Internal, service-to-service only — protected by the shared gateway secret. Called by catalog-service to build order-history-based recommendations. Excludes cancelled/returned orders.",
            tags = {"Internal"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Book ids returned (possibly empty)")
    })
    @GetMapping("/purchased-book-ids")
    public ResponseEntity<PurchasedBookIdsResponse> purchasedBookIds(@RequestParam UUID userId) {
        return ResponseEntity.ok(new PurchasedBookIdsResponse(orderItemRepository.findDistinctBookIdsByUserId(userId)));
    }

    @Operation(
            summary = "Get the caller's most recent order line items, with status",
            description = "Internal, service-to-service only — protected by the shared gateway secret. Called by catalog-service to back the \"Your orders\" rail. Newest-first, every order status included (cancelled/returned too — the status is the point).",
            tags = {"Internal"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recent order items returned (possibly empty)")
    })
    @GetMapping("/recent-items")
    public ResponseEntity<List<RecentOrderItemResponse>> recentItems(
            @RequestParam UUID userId,
            @RequestParam(defaultValue = "20") int limit
    ) {
        List<RecentOrderItemResponse> items = orderItemRepository.findRecentByUserId(userId, PageRequest.of(0, limit)).stream()
                .map(oi -> new RecentOrderItemResponse(oi.getBookId(), oi.getOrder().getStatus().name(), oi.getOrder().getPlacedAt()))
                .toList();
        return ResponseEntity.ok(items);
    }
}
