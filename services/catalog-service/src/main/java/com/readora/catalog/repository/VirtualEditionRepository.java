package com.readora.catalog.repository;

import com.readora.catalog.entity.VirtualEdition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

// Spring Data JPA repository for VirtualEdition.
public interface VirtualEditionRepository extends JpaRepository<VirtualEdition, UUID> {
}
