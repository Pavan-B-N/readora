package com.readora.catalog.repository;

import com.readora.catalog.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

// Spring Data JPA repository for Store.
public interface StoreRepository extends JpaRepository<Store, UUID> {

    // Returns all active stores, alphabetically by name.
    List<Store> findAllByIsActiveTrueOrderByName();
}
