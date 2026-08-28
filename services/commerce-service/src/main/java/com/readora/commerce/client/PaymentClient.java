package com.readora.commerce.client;

import com.readora.commerce.dto.PaymentDetails;
import com.readora.commerce.dto.RefundStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Direct service-to-service call to payment-service, bypassing api-gateway — same pattern as
 * every other internal client in this build. Best-effort: refund status is a display enrichment
 * for the admin returns view, not a security check, so a failure here degrades to "unknown"
 * rather than blocking the page (unlike UserServiceClient.getAdminStoreId, which fails closed).
 */
@Component
public class PaymentClient {

    private static final Logger log = LoggerFactory.getLogger(PaymentClient.class);

    private final RestClient restClient;

    public PaymentClient(
            @Value("${app.payment-service.base-url}") String baseUrl,
            @Value("${app.gateway.secret}") String gatewaySecret
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-Gateway-Secret", gatewaySecret)
                .build();
    }

    /** @return orderId -> refund status, for whichever of the requested orders have a refund recorded */
    public Map<UUID, RefundStatus> getRefundStatuses(List<UUID> orderIds) {
        if (orderIds.isEmpty()) {
            return Map.of();
        }
        try {
            List<RefundStatus> statuses = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/internal/refunds/by-order-ids").queryParam("orderIds", orderIds).build())
                    .retrieve()
                    .body(new org.springframework.core.ParameterizedTypeReference<List<RefundStatus>>() {
                    });
            return statuses == null ? Map.of() : statuses.stream().collect(Collectors.toMap(RefundStatus::orderId, Function.identity()));
        } catch (Exception e) {
            log.warn("Could not fetch refund statuses from payment-service for admin returns view", e);
            return Map.of();
        }
    }

    /**
     * Best-effort, same rationale as getRefundStatuses above: transaction info is a display
     * enrichment for the customer's order-detail page, not something that should block the page
     * from loading if payment-service is briefly unavailable. Empty also covers the normal case
     * of an order whose payment record hasn't been created yet (e.g. right after checkout, before
     * the order.created event has been consumed).
     */
    public Optional<PaymentDetails> getPaymentDetails(UUID orderId) {
        try {
            PaymentDetails details = restClient.get()
                    .uri("/internal/payments/{orderId}", orderId)
                    .retrieve()
                    .body(PaymentDetails.class);
            return Optional.ofNullable(details);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() != 404) {
                log.warn("Could not fetch payment details from payment-service for order {}", orderId, e);
            }
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Could not fetch payment details from payment-service for order {}", orderId, e);
            return Optional.empty();
        }
    }
}
