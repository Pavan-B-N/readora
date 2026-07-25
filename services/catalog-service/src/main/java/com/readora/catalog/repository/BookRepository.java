package com.readora.catalog.repository;

import com.readora.catalog.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

// Spring Data JPA repository for Book, plus a Specification executor for dynamic filtering.
public interface BookRepository extends JpaRepository<Book, UUID>, JpaSpecificationExecutor<Book> {

    // Returns books never embedded, or updated since their last embedding.
    @Query("SELECT b FROM Book b WHERE b.embeddedAt IS NULL OR b.updatedAt > b.embeddedAt")
    Page<Book> findNeedingReembedding(Pageable pageable);

    // Whether any book still references the given category.
    boolean existsByCategoryId(UUID categoryId);

    // Whether any book still credits the given author.
    boolean existsByAuthorsId(UUID authorId);

    // Whether a book with this ISBN-13 already exists.
    boolean existsByIsbn13(String isbn13);

    // Bulk update bypassing @PreUpdate — going through the entity would bump updatedAt again on flush and make the book look stale as soon as it's marked fresh, defeating the point of tracking embeddedAt vs updatedAt.
    @Modifying
    @Query("UPDATE Book b SET b.embeddedAt = :at WHERE b.id IN :ids")
    void markEmbedded(@Param("ids") List<UUID> ids, @Param("at") Instant at);
}
