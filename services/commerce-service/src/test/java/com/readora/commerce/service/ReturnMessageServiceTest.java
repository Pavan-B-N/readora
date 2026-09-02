package com.readora.commerce.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.readora.commerce.client.UserServiceClient;
import com.readora.commerce.entity.DeliveryType;
import com.readora.commerce.entity.Order;
import com.readora.commerce.entity.OrderStatus;
import com.readora.commerce.entity.PaymentMethod;
import com.readora.commerce.entity.ReturnSenderRole;
import com.readora.commerce.exception.ReturnNotUnderReviewException;
import com.readora.commerce.repository.OutboxEventRepository;
import com.readora.commerce.repository.ReturnMessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReturnMessageServiceTest {

    @Mock private ReturnMessageRepository repository;
    @Mock private UserServiceClient userServiceClient;
    @Mock private OutboxEventRepository outboxEventRepository;

    private ReturnMessageService service;

    @BeforeEach
    void setUp() {
        service = new ReturnMessageService(repository, userServiceClient, outboxEventRepository, new ObjectMapper());
    }

    private static Order order(OrderStatus status) {
        Order order = new Order("RDA-2026-000001", UUID.randomUUID(), "INR", new BigDecimal("100.00"), BigDecimal.ZERO,
                BigDecimal.ZERO, new BigDecimal("9.00"), new BigDecimal("109.00"), BigDecimal.ZERO, PaymentMethod.WALLET,
                UUID.randomUUID().toString(), DeliveryType.PHYSICAL);
        ReflectionTestUtils.setField(order, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(order, "placedAt", Instant.now());
        order.transitionTo(status);
        return order;
    }

    @Test
    void post_orderNotUnderReview_throws() {
        Order order = order(OrderStatus.DELIVERED);

        assertThatThrownBy(() -> service.post(order, UUID.randomUUID(), ReturnSenderRole.CUSTOMER, "hi"))
                .isInstanceOf(ReturnNotUnderReviewException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void post_fromCustomer_notifiesStoreAdmin() {
        Order order = order(OrderStatus.RETURN_REQUESTED);
        order.setStoreId(UUID.randomUUID());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userServiceClient.getAdminUserIdForStore(order.getStoreId())).thenReturn(UUID.randomUUID());

        service.post(order, UUID.randomUUID(), ReturnSenderRole.CUSTOMER, "Where is my refund?");

        verify(userServiceClient).getAdminUserIdForStore(order.getStoreId());
        verify(outboxEventRepository).save(any());
    }

    @Test
    void post_fromAdmin_notifiesCustomerWithoutLookingUpStoreAdmin() {
        Order order = order(OrderStatus.RETURN_REQUESTED);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.post(order, UUID.randomUUID(), ReturnSenderRole.ADMIN, "We're looking into it");

        verify(userServiceClient, never()).getAdminUserIdForStore(any());
        verify(outboxEventRepository).save(any());
    }

    @Test
    void post_noAdminAssignedToStore_skipsNotificationWithoutFailing() {
        Order order = order(OrderStatus.RETURN_REQUESTED);
        order.setStoreId(UUID.randomUUID());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userServiceClient.getAdminUserIdForStore(any())).thenReturn(null);

        service.post(order, UUID.randomUUID(), ReturnSenderRole.CUSTOMER, "hello");

        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    void list_mapsRepositoryResults() {
        when(repository.findAllByOrderIdOrderByCreatedAt(any())).thenReturn(List.of());

        assertThat(service.list(UUID.randomUUID())).isEmpty();
    }
}
