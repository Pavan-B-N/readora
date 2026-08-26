package com.readora.mcp.client;

import com.readora.mcp.dto.ProfileInfo;
import com.readora.mcp.dto.WalletInfo;
import com.readora.mcp.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Forwards the caller's userId as a freshly minted internal JWT — user-service validates it
 * itself and extracts the userId, the same as it would for a real user token.
 */
@Component
public class UserClient {

    private final RestClient restClient;
    private final JwtService jwtService;

    public UserClient(
            @Value("${app.user-service.base-url}") String baseUrl,
            @Value("${app.gateway.secret}") String gatewaySecret,
            JwtService jwtService
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-Gateway-Secret", gatewaySecret)
                .build();
        this.jwtService = jwtService;
    }

    public ProfileInfo getProfile(String userId) {
        return restClient.get()
                .uri("/api/v1/users/me")
                .header("Authorization", "Bearer " + jwtService.issueInternalToken(userId))
                .retrieve()
                .body(ProfileInfo.class);
    }

    public WalletInfo getWallet(String userId) {
        return restClient.get()
                .uri("/api/v1/users/me/wallet")
                .header("Authorization", "Bearer " + jwtService.issueInternalToken(userId))
                .retrieve()
                .body(WalletInfo.class);
    }
}
