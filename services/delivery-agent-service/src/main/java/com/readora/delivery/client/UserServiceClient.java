package com.readora.delivery.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.readora.delivery.dto.AdminStoreResponse;
import com.readora.delivery.exception.ServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.UUID;

/** Direct service-to-service calls to user-service, bypassing api-gateway — same pattern as CommerceClient. */
@Component
public class UserServiceClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public UserServiceClient(
            @Value("${app.user-service.base-url}") String baseUrl,
            @Value("${app.gateway.secret}") String gatewaySecret,
            ObjectMapper objectMapper
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-Gateway-Secret", gatewaySecret)
                .build();
        this.objectMapper = objectMapper;
    }

    /**
     * Resolves which store an admin is scoped to — fail-closed: any error here propagates rather
     * than being treated as "no store," so an admin can never fall through to seeing every
     * store's agents just because user-service hiccuped.
     */
    public UUID getAdminStoreId(UUID userId) {
        try {
            AdminStoreResponse response = restClient.get()
                    .uri("/internal/admin-users/{userId}/store", userId)
                    .retrieve()
                    .body(AdminStoreResponse.class);
            return response != null ? response.storeId() : null;
        } catch (Exception e) {
            throw translate(e);
        }
    }

    private RuntimeException translate(Exception e) {
        if (e instanceof RestClientResponseException responseException) {
            HttpStatus status = HttpStatus.resolve(responseException.getStatusCode().value());
            String message = extractMessage(responseException.getResponseBodyAsString());
            return new ServiceException(
                    "USER_SERVICE_REJECTED",
                    status != null ? status : HttpStatus.BAD_GATEWAY,
                    message != null ? message : "User service rejected the request"
            );
        }
        if (e instanceof ResourceAccessException) {
            return new ServiceException(
                    "USER_SERVICE_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE, "User service is currently unavailable"
            );
        }
        return new ServiceException(
                "USER_SERVICE_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE, "User service is currently unavailable"
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
