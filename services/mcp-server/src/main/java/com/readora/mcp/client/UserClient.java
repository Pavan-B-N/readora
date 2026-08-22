package com.readora.mcp.client;

import com.readora.mcp.dto.ProfileInfo;
import com.readora.mcp.dto.WalletInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class UserClient {

    private final RestClient restClient;

    public UserClient(
            @Value("${app.user-service.base-url}") String baseUrl,
            @Value("${app.gateway.secret}") String gatewaySecret
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-Gateway-Secret", gatewaySecret)
                .build();
    }

    public ProfileInfo getProfile(String userId) {
        return restClient.get()
                .uri("/api/v1/users/me")
                .header("X-User-Id", userId)
                .retrieve()
                .body(ProfileInfo.class);
    }

    public WalletInfo getWallet(String userId) {
        return restClient.get()
                .uri("/api/v1/users/me/wallet")
                .header("X-User-Id", userId)
                .retrieve()
                .body(WalletInfo.class);
    }
}
