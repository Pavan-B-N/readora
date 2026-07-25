package com.readora.catalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** A book in the catalogue. No embedding/pgvector column — that's ai-service scope. */
@Entity
@Table(name = "books", schema = "catalog")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "isbn13", nullable = false, unique = true, length = 13)
    private String isbn13;

    @Column(name = "title", nullable = false)
    private String title;


    @Column(name = "description", columnDefinition = "text")
    private String description;

    // Nested topic breakdown as a JSON string; only meaningful for non-fiction/technical books, left null/empty for fiction, and only ever flattened into text for ai-service's embedding pipeline (not queried structurally), so plain text storage is enough.
    @Column(name = "table_of_contents", columnDefinition = "text")
    private String tableOfContents;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "publisher_id")
    private Publisher publisher;

    // The store that stocks the physical copy — null means this book exists only as a virtual edition, universally available regardless of which store the customer is shopping (see CatalogService.search).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "book_authors",
            schema = "catalog",
            joinColumns = @JoinColumn(name = "book_id"),
            inverseJoinColumns = @JoinColumn(name = "author_id")
    )
    private Set<Author> authors = new HashSet<>();

    @Column(name = "language")
    private String language;

    @Column(name = "page_count")
    private Integer pageCount;

    @Column(name = "published_on")
    private LocalDate publishedOn;

    @Column(name = "list_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal listPrice;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "cover_image_url")
    private String coverImageUrl;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** When ai-service last (re-)embedded this book. Null means never embedded. */
    @Column(name = "embedded_at")
    private Instant embeddedAt;

    /** Admin user who created this listing — audit trail, not enforced/validated. */
    @Column(name = "created_by_user_id")
    private UUID createdByUserId;

    // No-arg constructor required by JPA.
    protected Book() {
    }

    // Creates a new book with the given catalogue details.
    public Book(
            String isbn13, String title, String description, Category category,
            Publisher publisher, Store store, String language, Integer pageCount,
            LocalDate publishedOn, BigDecimal listPrice, String currency, String coverImageUrl,
            UUID createdByUserId
    ) {
        this.isbn13 = isbn13;
        this.title = title;
        this.description = description;
        this.category = category;
        this.publisher = publisher;
        this.store = store;
        this.language = language;
        this.pageCount = pageCount;
        this.publishedOn = publishedOn;
        this.listPrice = listPrice;
        this.currency = currency;
        this.coverImageUrl = coverImageUrl;
        this.createdByUserId = createdByUserId;
    }

    // Stamps createdAt/updatedAt before the initial insert.
    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    // Refreshes updatedAt before every update.
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // Adds a single author to this book's author set.
    public void addAuthor(Author author) {
        this.authors.add(author);
    }

    /** @param newAuthors the full author set to replace the current one with */
    public void replaceAuthors(Set<Author> newAuthors) {
        this.authors.clear();
        this.authors.addAll(newAuthors);
    }

    // Applies an admin update — every field is set, not merged; callers should send the current value for anything they don't intend to change.
    public void update(
            String title, String description, String tableOfContents,
            Category category, Publisher publisher, String language,
            Integer pageCount, LocalDate publishedOn, BigDecimal listPrice, String currency,
            String coverImageUrl, boolean isActive
    ) {
        this.title = title;
        this.description = description;
        this.tableOfContents = tableOfContents;
        this.category = category;
        this.publisher = publisher;
        this.language = language;
        this.pageCount = pageCount;
        this.publishedOn = publishedOn;
        this.listPrice = listPrice;
        this.currency = currency;
        this.coverImageUrl = coverImageUrl;
        this.isActive = isActive;
    }

    // Returns the book's id.
    public UUID getId() {
        return id;
    }

    // Returns the book's ISBN-13.
    public String getIsbn13() {
        return isbn13;
    }

    // Returns the book's title.
    public String getTitle() {
        return title;
    }

    // Returns the book's description.
    public String getDescription() {
        return description;
    }

    /** @return the nested table-of-contents JSON string, or null if not set */
    public String getTableOfContents() {
        return tableOfContents;
    }

    /** @param tableOfContents the nested table-of-contents JSON string to set */
    public void setTableOfContents(String tableOfContents) {
        this.tableOfContents = tableOfContents;
    }

    // Returns the book's category, or null if uncategorized.
    public Category getCategory() {
        return category;
    }

    // Returns the book's publisher, or null if unset.
    public Publisher getPublisher() {
        return publisher;
    }

    // Returns the store that stocks this book's physical copy, or null for a virtual-only book.
    public Store getStore() {
        return store;
    }

    // Returns the book's credited authors.
    public Set<Author> getAuthors() {
        return authors;
    }

    // Returns the book's language.
    public String getLanguage() {
        return language;
    }

    // Returns the book's page count.
    public Integer getPageCount() {
        return pageCount;
    }

    // Returns the book's publication date.
    public LocalDate getPublishedOn() {
        return publishedOn;
    }

    // Returns the book's list price.
    public BigDecimal getListPrice() {
        return listPrice;
    }

    // Returns the book's currency code.
    public String getCurrency() {
        return currency;
    }

    // Returns the book's cover image URL.
    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    // Whether the book is currently active/listed.
    public boolean isActive() {
        return isActive;
    }

    // Deactivates the book so it's no longer listed.
    public void deactivate() {
        this.isActive = false;
    }

    // Returns when the book was created.
    public Instant getCreatedAt() {
        return createdAt;
    }

    // Returns when ai-service last embedded this book, or null if never embedded.
    public Instant getEmbeddedAt() {
        return embeddedAt;
    }

    // Returns the id of the admin who created this listing.
    public UUID getCreatedByUserId() {
        return createdByUserId;
    }

    /** True when this book has never been embedded, or has changed since its last embedding. */
    public boolean needsReembedding() {
        return embeddedAt == null || updatedAt.isAfter(embeddedAt);
    }

    // Stamps embeddedAt with the given time.
    public void markEmbedded(Instant at) {
        this.embeddedAt = at;
    }

    // Entities are equal when they share a non-null id.
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Book book)) return false;
        return id != null && Objects.equals(id, book.id);
    }

    // Hashes by id, consistent with equals.
    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
