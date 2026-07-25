package com.readora.catalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

// userId is a cross-service reference to auth.User (plain UUID, never a JPA relationship, same convention as UserProfile.userId); authorDisplayName is a write-time snapshot via UserServiceClient rather than a live lookup — same reasoning as OrderShippingAddress.recipientName, trading staleness on rename for avoiding a cross-service call on every review read.
@Entity
@Table(name = "reviews", schema = "catalog", uniqueConstraints = @UniqueConstraint(columnNames = {"book_id", "user_id"}))
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "author_display_name")
    private String authorDisplayName;

    @Column(name = "rating", nullable = false)
    private int rating;

    @Column(name = "comment", columnDefinition = "text")
    private String comment;

    /** Whether the reviewer had a non-cancelled/returned order containing this book, at the time they reviewed it. */
    @Column(name = "verified_purchase", nullable = false)
    private boolean verifiedPurchase;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // No-arg constructor required by JPA.
    protected Review() {
    }

    // Creates a new review with the given details.
    public Review(Book book, UUID userId, String authorDisplayName, int rating, String comment, boolean verifiedPurchase) {
        this.book = book;
        this.userId = userId;
        this.authorDisplayName = authorDisplayName;
        this.rating = rating;
        this.comment = comment;
        this.verifiedPurchase = verifiedPurchase;
    }

    // Stamps createdAt and updatedAt before insert.
    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    // Stamps updatedAt before update.
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // Applied when a user resubmits a review for a book they've already reviewed — upsert, not a new row.
    public void update(int rating, String comment, boolean verifiedPurchase) {
        this.rating = rating;
        this.comment = comment;
        this.verifiedPurchase = verifiedPurchase;
    }

    // Returns the review's id.
    public UUID getId() {
        return id;
    }

    // Returns the id of the reviewed book.
    public UUID getBookId() {
        return book.getId();
    }

    // Returns the reviewer's user id.
    public UUID getUserId() {
        return userId;
    }

    // Returns the reviewer's display name as snapshotted at write time.
    public String getAuthorDisplayName() {
        return authorDisplayName;
    }

    // Returns the star rating.
    public int getRating() {
        return rating;
    }

    // Returns the review comment text.
    public String getComment() {
        return comment;
    }

    // Whether the reviewer had a verified purchase of this book at review time.
    public boolean isVerifiedPurchase() {
        return verifiedPurchase;
    }

    // Returns when the review was created.
    public Instant getCreatedAt() {
        return createdAt;
    }

    // Entities are equal when they share a non-null id.
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Review review)) return false;
        return id != null && Objects.equals(id, review.id);
    }

    // Hashes by id, consistent with equals.
    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
