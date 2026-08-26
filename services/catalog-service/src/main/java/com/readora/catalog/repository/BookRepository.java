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
import java.util.Set;
import java.util.UUID;

public interface BookRepository extends JpaRepository<Book, UUID>, JpaSpecificationExecutor<Book> {

    List<Book> findAllByCategory_IdInAndIdNotInAndIsActiveTrue(Set<UUID> categoryIds, Set<UUID> excludeBookIds, Pageable pageable);

    /** Backs the typeahead suggest endpoint — cheap substring match, capped by the caller's Pageable. */
    List<Book> findAllByIsActiveTrueAndTitleContainingIgnoreCase(String title, Pageable pageable);

    @Query("SELECT b FROM Book b WHERE b.embeddedAt IS NULL OR b.updatedAt > b.embeddedAt")
    Page<Book> findNeedingReembedding(Pageable pageable);

    /**
     * Bulk update, bypassing {@code @PreUpdate} — going through the entity would bump
     * {@code updatedAt} again on flush and make the book look stale as soon as it's marked
     * fresh, defeating the whole point of tracking embeddedAt vs updatedAt.
     */
    @Modifying
    @Query("UPDATE Book b SET b.embeddedAt = :at WHERE b.id IN :ids")
    void markEmbedded(@Param("ids") List<UUID> ids, @Param("at") Instant at);
}
