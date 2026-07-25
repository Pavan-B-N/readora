package com.readora.catalog.repository;

import com.readora.catalog.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

// Spring Data JPA repository for Inventory.
public interface InventoryRepository extends JpaRepository<Inventory, UUID> {
}
