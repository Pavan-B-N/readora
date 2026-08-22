package com.readora.catalog.repository;

import com.readora.catalog.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    List<Category> findAllByParentIsNullOrderByDisplayOrder();

    List<Category> findAllByParentIdOrderByDisplayOrder(UUID parentId);
}
