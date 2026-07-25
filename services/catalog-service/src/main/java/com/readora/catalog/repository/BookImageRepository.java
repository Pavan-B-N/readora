package com.readora.catalog.repository;

import com.readora.catalog.entity.BookImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

// Spring Data JPA repository for BookImage.
public interface BookImageRepository extends JpaRepository<BookImage, UUID> {

    // Returns a book's images in display order.
    List<BookImage> findAllByBookIdOrderBySortOrder(UUID bookId);
}
