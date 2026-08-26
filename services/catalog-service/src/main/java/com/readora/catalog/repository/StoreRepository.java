package com.readora.catalog.repository;

import com.readora.catalog.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StoreRepository extends JpaRepository<Store, UUID> {

    List<Store> findAllByIsActiveTrueOrderByName();
}
