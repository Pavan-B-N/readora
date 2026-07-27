package com.readora.catalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

// The digital edition of a book, if one is available — book_id is both PK and FK (shared primary key via @MapsId, same pattern as Inventory); file_url is a reference/key, not a permanently public link, so fulfillment should generate a signed, expiring URL from it rather than hand out this value directly.
@Entity
@Table(name = "virtual_editions", schema = "catalog")
public class VirtualEdition {

    @Id
    @Column(name = "book_id")
    private UUID bookId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "book_id")
    private Book book;

    @Column(name = "file_url", nullable = false)
    private String fileUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_format", nullable = false)
    private VirtualFileFormat fileFormat;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    /** Which admin added this edition — any admin from any store may, but it's tracked. Never overwritten by later updates. */
    @Column(name = "created_by_user_id")
    private UUID createdByUserId;

    // No-arg constructor required by JPA.
    protected VirtualEdition() {
    }

    // Creates a new active virtual edition for a book.
    public VirtualEdition(
            Book book, String fileUrl, VirtualFileFormat fileFormat, Long fileSizeBytes, BigDecimal price,
            String currency, UUID createdByUserId
    ) {
        this.book = book;
        this.fileUrl = fileUrl;
        this.fileFormat = fileFormat;
        this.fileSizeBytes = fileSizeBytes;
        this.price = price;
        this.currency = currency;
        this.createdByUserId = createdByUserId;
    }

    // Returns the id of the book this edition belongs to.
    public UUID getBookId() {
        return bookId;
    }

    // Returns the opaque storage key/reference for the file.
    public String getFileUrl() {
        return fileUrl;
    }

    // Returns the file format.
    public VirtualFileFormat getFileFormat() {
        return fileFormat;
    }

    // Returns the file size in bytes, if known.
    public Long getFileSizeBytes() {
        return fileSizeBytes;
    }

    // Returns the price.
    public BigDecimal getPrice() {
        return price;
    }

    // Returns the currency code.
    public String getCurrency() {
        return currency;
    }

    // Whether the edition is currently active/purchasable.
    public boolean isActive() {
        return isActive;
    }

    // Deactivates this edition so it's no longer purchasable.
    public void deactivate() {
        this.isActive = false;
    }

    // Returns the id of the admin who created this edition.
    public UUID getCreatedByUserId() {
        return createdByUserId;
    }

    // Applies an admin update to an existing edition, and reactivates it if it was deactivated.
    public void update(String fileUrl, VirtualFileFormat fileFormat, Long fileSizeBytes, BigDecimal price, String currency) {
        this.fileUrl = fileUrl;
        this.fileFormat = fileFormat;
        this.fileSizeBytes = fileSizeBytes;
        this.price = price;
        this.currency = currency;
        this.isActive = true;
    }

    // Entities are equal when they share a non-null bookId.
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof VirtualEdition that)) return false;
        return bookId != null && Objects.equals(bookId, that.bookId);
    }

    // Hashes by bookId, consistent with equals.
    @Override
    public int hashCode() {
        return Objects.hashCode(bookId);
    }
}
