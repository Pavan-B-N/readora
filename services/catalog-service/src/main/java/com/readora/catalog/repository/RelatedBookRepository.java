package com.readora.catalog.repository;

import com.readora.catalog.entity.RelatedBook;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RelatedBookRepository extends JpaRepository<RelatedBook, UUID> {

    List<RelatedBook> findAllByBookId(UUID bookId);
}
