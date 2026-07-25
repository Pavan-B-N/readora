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

// JPA entity for one image belonging to a book.
@Entity
@Table(name = "book_images", schema = "catalog")
public class BookImage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(name = "url", nullable = false)
    private String url;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    // No-arg constructor required by JPA.
    protected BookImage() {
    }

    // Creates a new image entry for a book.
    public BookImage(Book book, String url, int sortOrder) {
        this.book = book;
        this.url = url;
        this.sortOrder = sortOrder;
    }

    // Returns the image's id.
    public UUID getId() {
        return id;
    }

    // Returns the image's URL.
    public String getUrl() {
        return url;
    }

    // Returns the image's display order among its book's images.
    public int getSortOrder() {
        return sortOrder;
    }

    // Entities are equal when they share a non-null id.
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof BookImage bookImage)) return false;
        return id != null && Objects.equals(id, bookImage.id);
    }

    // Hashes by id, consistent with equals.
    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
