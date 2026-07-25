package com.readora.catalog.repository;

import com.readora.catalog.entity.RelatedBook;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

// Spring Data JPA repository for RelatedBook.
public interface RelatedBookRepository extends JpaRepository<RelatedBook, UUID> {

    // Returns all related-book entries for the given book.
    List<RelatedBook> findAllByBookId(UUID bookId);
}
