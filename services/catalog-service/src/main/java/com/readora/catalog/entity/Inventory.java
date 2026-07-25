package com.readora.catalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

// book_id is both the PK and a real FK to Book (shared primary key via @MapsId), unlike the plain-UUID cross-service references used elsewhere, since Book lives in the same schema; available = qtyOnHand - qtyReserved.
@Entity
@Table(name = "inventory", schema = "catalog")
public class Inventory {

    @Id
    @Column(name = "book_id")
    private UUID bookId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "book_id")
    private Book book;

    @Column(name = "qty_on_hand", nullable = false)
    private int qtyOnHand;

    @Column(name = "qty_reserved", nullable = false)
    private int qtyReserved;

    @Column(name = "reorder_threshold")
    private int reorderThreshold;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // No-arg constructor required by JPA.
    protected Inventory() {
    }

    // Creates a new inventory row for a book, with zero reserved.
    public Inventory(Book book, int qtyOnHand, int reorderThreshold) {
        this.book = book;
        this.qtyOnHand = qtyOnHand;
        this.reorderThreshold = reorderThreshold;
    }

    // Stamps updatedAt before every insert/update.
    @PrePersist
    @PreUpdate
    protected void onSave() {
        this.updatedAt = Instant.now();
    }

    // Returns the quantity available to sell (on hand minus reserved).
    public int getAvailable() {
        return qtyOnHand - qtyReserved;
    }

    // Reserves the given quantity if enough is available, returning whether it succeeded.
    public boolean reserve(int qty) {
        if (getAvailable() < qty) {
            return false;
        }
        this.qtyReserved += qty;
        return true;
    }

    // Admin correction — sets the absolute on-hand quantity and reorder threshold, without touching qtyReserved.
    public void restock(int qtyOnHand, int reorderThreshold) {
        this.qtyOnHand = qtyOnHand;
        this.reorderThreshold = reorderThreshold;
    }

    // Returns the book's id (also this entity's PK).
    public UUID getBookId() {
        return bookId;
    }

    // Returns the on-hand quantity.
    public int getQtyOnHand() {
        return qtyOnHand;
    }

    // Returns the reserved quantity.
    public int getQtyReserved() {
        return qtyReserved;
    }

    // Returns the reorder threshold.
    public int getReorderThreshold() {
        return reorderThreshold;
    }

    // Entities are equal when they share a non-null bookId.
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Inventory inventory)) return false;
        return bookId != null && Objects.equals(bookId, inventory.bookId);
    }

    // Hashes by bookId, consistent with equals.
    @Override
    public int hashCode() {
        return Objects.hashCode(bookId);
    }
}
