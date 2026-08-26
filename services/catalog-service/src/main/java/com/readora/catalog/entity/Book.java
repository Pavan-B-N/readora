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

    @Column(name = "subtitle")
    private String subtitle;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    /**
     * Nested topic breakdown, stored as a JSON string (e.g. {"Basics": ["Variables", "Loops"],
     * "OOP": ["Classes", "Inheritance"]}). Only meaningful for non-fiction/technical books —
     * nullable, and left empty for fiction rather than filled with placeholder content that
     * would dilute its embedding. Not queried structurally, only flattened into text for
     * ai-service's embedding pipeline, so plain text storage (not a relational entity) is enough.
     */
    @Column(name = "table_of_contents", columnDefinition = "text")
    private String tableOfContents;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "publisher_id")
    private Publisher publisher;

    /**
     * The store that stocks the physical copy — null means this book has no physical presence
     * anywhere and exists only as a virtual edition, universally available regardless of which
     * store the customer is shopping. A book with a store may still also carry a virtual
     * edition; that edition stays store-independent either way (see CatalogService.search).
     */
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

    protected Book() {
    }

    public Book(
            String isbn13, String title, String subtitle, String description, Category category,
            Publisher publisher, Store store, String language, Integer pageCount,
            LocalDate publishedOn, BigDecimal listPrice, String currency, String coverImageUrl,
            UUID createdByUserId
    ) {
        this.isbn13 = isbn13;
        this.title = title;
        this.subtitle = subtitle;
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

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void addAuthor(Author author) {
        this.authors.add(author);
    }

    /** @param newAuthors the full author set to replace the current one with */
    public void replaceAuthors(Set<Author> newAuthors) {
        this.authors.clear();
        this.authors.addAll(newAuthors);
    }

    /**
     * Applies an admin update — every field is set, not merged; callers should send the
     * current value for anything they don't intend to change.
     */
    public void update(
            String title, String subtitle, String description, String tableOfContents,
            Category category, Publisher publisher, String language,
            Integer pageCount, LocalDate publishedOn, BigDecimal listPrice, String currency,
            String coverImageUrl, boolean isActive
    ) {
        this.title = title;
        this.subtitle = subtitle;
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

    public UUID getId() {
        return id;
    }

    public String getIsbn13() {
        return isbn13;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

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

    public Category getCategory() {
        return category;
    }

    public Publisher getPublisher() {
        return publisher;
    }

    public Store getStore() {
        return store;
    }

    public Set<Author> getAuthors() {
        return authors;
    }

    public String getLanguage() {
        return language;
    }

    public Integer getPageCount() {
        return pageCount;
    }

    public LocalDate getPublishedOn() {
        return publishedOn;
    }

    public BigDecimal getListPrice() {
        return listPrice;
    }

    public String getCurrency() {
        return currency;
    }

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public boolean isActive() {
        return isActive;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getEmbeddedAt() {
        return embeddedAt;
    }

    public UUID getCreatedByUserId() {
        return createdByUserId;
    }

    /** True when this book has never been embedded, or has changed since its last embedding. */
    public boolean needsReembedding() {
        return embeddedAt == null || updatedAt.isAfter(embeddedAt);
    }

    public void markEmbedded(Instant at) {
        this.embeddedAt = at;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Book book)) return false;
        return id != null && Objects.equals(id, book.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
