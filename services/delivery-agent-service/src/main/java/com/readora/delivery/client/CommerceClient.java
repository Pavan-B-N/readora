package com.readora.delivery.client;

import com.readora.delivery.dto.OrderDeliveryDetailResponse;
import com.readora.delivery.dto.UpdateDeliveryStatusRequest;
import com.readora.delivery.exception.ServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

/**
 * Direct service-to-service calls to commerce-service, bypassing api-gateway — same pattern as
 * catalog-service's internal clients. commerce-service's Order.status stays the source of truth;
 * this service only ever reads it or asks it to advance.
 */
@Component
public class CommerceClient {

    private final RestClient restClient;

    public CommerceClient(
            @Value("${app.commerce-service.base-url}") String baseUrl,
            @Value("${app.gateway.secret}") String gatewaySecret
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-Gateway-Secret", gatewaySecret)
                .build();
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

    private RuntimeException translate(Exception e) {
        return new ServiceException(
                "COMMERCE_SERVICE_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE, "Commerce service is currently unavailable"
        );
    }
}
