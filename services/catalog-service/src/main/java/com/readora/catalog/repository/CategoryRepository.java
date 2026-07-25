package com.readora.catalog.repository;

import com.readora.catalog.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

// Spring Data JPA repository for Category.
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    // Returns all categories in display order.
    List<Category> findAllByOrderByDisplayOrder();
}
