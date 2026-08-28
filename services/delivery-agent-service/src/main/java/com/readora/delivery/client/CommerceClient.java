package com.readora.delivery.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.readora.delivery.dto.OrderDeliveryDetailResponse;
import com.readora.delivery.dto.UpdateDeliveryStatusRequest;
import com.readora.delivery.dto.UpdateReturnStatusRequest;
import com.readora.delivery.exception.ServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.UUID;

/**
 * Direct service-to-service calls to commerce-service, bypassing api-gateway — same pattern as
 * catalog-service's internal clients. commerce-service's Order.status stays the source of truth;
 * this service only ever reads it or asks it to advance.
 */
@Component
public class CommerceClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public CommerceClient(
            @Value("${app.commerce-service.base-url}") String baseUrl,
            @Value("${app.gateway.secret}") String gatewaySecret,
            ObjectMapper objectMapper
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-Gateway-Secret", gatewaySecret)
                .build();
        this.objectMapper = objectMapper;
    }

    public OrderDeliveryDetailResponse getDeliveryDetail(UUID orderId) {
        try {
            return restClient.get()
                    .uri("/internal/orders/{id}/delivery-detail", orderId)
                    .retrieve()
                    .body(OrderDeliveryDetailResponse.class);
        } catch (Exception e) {
            throw translate(e);
        }
    }

    public void updateDeliveryStatus(UUID orderId, String status, UUID deliveryAgentId, String deliveryAgentName) {
        try {
            restClient.put()
                    .uri("/internal/orders/{id}/delivery-status", orderId)
                    .body(new UpdateDeliveryStatusRequest(status, deliveryAgentId, deliveryAgentName))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            throw translate(e);
        }
    }

    public void updateReturnStatus(UUID orderId, String status, UUID returnAgentId, String returnAgentName) {
        try {
            restClient.put()
                    .uri("/internal/orders/{id}/return-status", orderId)
                    .body(new UpdateReturnStatusRequest(status, returnAgentId, returnAgentName))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            throw translate(e);
        }
    }

    /**
     * Distinguishes a real rejection from commerce-service (its own status code and message,
     * passed straight through — e.g. a 409 because the order already moved past this status) from
     * an actual connectivity failure (commerce-service unreachable/timed out), which alone
     * deserves the generic "unavailable" message. Collapsing both into 503 previously made every
     * upstream validation error look like an outage.
     */
    private RuntimeException translate(Exception e) {
        if (e instanceof RestClientResponseException responseException) {
            HttpStatus status = HttpStatus.resolve(responseException.getStatusCode().value());
            String message = extractMessage(responseException.getResponseBodyAsString());
            return new ServiceException(
                    "COMMERCE_SERVICE_REJECTED",
                    status != null ? status : HttpStatus.BAD_GATEWAY,
                    message != null ? message : "Commerce service rejected the request"
            );
        }
        if (e instanceof ResourceAccessException) {
            return new ServiceException(
                    "COMMERCE_SERVICE_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE, "Commerce service is currently unavailable"
            );
        }
        return new ServiceException(
                "COMMERCE_SERVICE_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE, "Commerce service is currently unavailable"
        );
    }

    private String extractMessage(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) return null;
        try {
            JsonNode node = objectMapper.readTree(responseBody);
            JsonNode messageNode = node.get("message");
            return messageNode != null ? messageNode.asText() : null;
        } catch (Exception parseFailure) {
            return null;
        }
    }
}
