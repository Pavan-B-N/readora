package com.readora.commerce.repository;

import com.readora.commerce.entity.ReturnMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ReturnMessageRepository extends JpaRepository<ReturnMessage, UUID> {

    @Query("SELECT m FROM ReturnMessage m WHERE m.order.id = :orderId ORDER BY m.createdAt")
    List<ReturnMessage> findAllByOrderIdOrderByCreatedAt(@Param("orderId") UUID orderId);
}
