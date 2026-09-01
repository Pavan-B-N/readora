package com.readora.commerce.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every method here degrades gracefully on failure by design (best-effort, per the class
 * javadoc) — no resilience4j proxy needed to exercise that, an unreachable address is enough to
 * hit the real catch blocks directly.
 */
class PaymentClientTest {

    private PaymentClient client;

    @BeforeEach
    void setUp() {
        client = new PaymentClient("http://127.0.0.1:1", "gateway-secret");
    }

    @Test
    void getRefundStatuses_emptyInput_returnsEmptyWithoutCallingOut() {
        assertThat(client.getRefundStatuses(List.of())).isEmpty();
    }

    @Test
    void getRefundStatuses_unreachable_degradesToEmptyMap() {
        assertThat(client.getRefundStatuses(List.of(UUID.randomUUID()))).isEmpty();
    }

    @Test
    void getPaymentDetails_unreachable_degradesToEmptyOptional() {
        assertThat(client.getPaymentDetails(UUID.randomUUID())).isEmpty();
    }
}
