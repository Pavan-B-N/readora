package com.readora.catalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.Objects;
import java.util.UUID;

// JPA entity linking a book to a related book (e.g. same series, same author).
@Entity
@Table(name = "related_books", schema = "catalog")
public class RelatedBook {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "related_book_id", nullable = false)
    private Book relatedBook;

    @Column(name = "relation_type")
    private String relationType;

    // No-arg constructor required by JPA.
    protected RelatedBook() {
    }

    // Creates a new relation between a book and a related book.
    public RelatedBook(Book book, Book relatedBook, String relationType) {
        this.book = book;
        this.relatedBook = relatedBook;
        this.relationType = relationType;
    }

    // Returns the related book.
    public Book getRelatedBook() {
        return relatedBook;
    }

    // Returns the type of relation (e.g. series, author).
    public String getRelationType() {
        return relationType;
    }

    // Entities are equal when they share a non-null id.
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof RelatedBook that)) return false;
        return id != null && Objects.equals(id, that.id);
    }

    // Hashes by id, consistent with equals.
    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
