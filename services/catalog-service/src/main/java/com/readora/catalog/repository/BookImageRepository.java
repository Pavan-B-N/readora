package com.readora.catalog.repository;

import com.readora.catalog.entity.BookImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BookImageRepository extends JpaRepository<BookImage, UUID> {

    List<BookImage> findAllByBookIdOrderBySortOrder(UUID bookId);
}
