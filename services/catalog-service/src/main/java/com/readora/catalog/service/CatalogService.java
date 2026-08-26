package com.readora.catalog.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.readora.catalog.client.CommerceClient;
import com.readora.catalog.dto.AuthorResponse;
import com.readora.catalog.dto.BookDetailResponse;
import com.readora.catalog.dto.BookSuggestionResponse;
import com.readora.catalog.dto.BookSummaryResponse;
import com.readora.catalog.dto.CategoryResponse;
import com.readora.catalog.dto.PageResponse;
import com.readora.catalog.dto.PublisherResponse;
import com.readora.catalog.dto.RelatedBookResponse;
import com.readora.catalog.entity.Author;
import com.readora.catalog.entity.Book;
import com.readora.catalog.entity.BookImage;
import com.readora.catalog.entity.Category;
import com.readora.catalog.entity.Inventory;
import com.readora.catalog.entity.RelatedBook;
import com.readora.catalog.entity.VirtualEdition;
import com.readora.catalog.exception.BookNotFoundException;
import com.readora.catalog.repository.AuthorRepository;
import com.readora.catalog.repository.BookImageRepository;
import com.readora.catalog.repository.BookRepository;
import com.readora.catalog.repository.BookSpecifications;
import com.readora.catalog.repository.CategoryRepository;
import com.readora.catalog.repository.InventoryRepository;
import com.readora.catalog.repository.PublisherRepository;
import com.readora.catalog.repository.RelatedBookRepository;
import com.readora.catalog.repository.ReviewRepository;
import com.readora.catalog.repository.VirtualEditionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CatalogService {

    private final BookRepository bookRepository;
    private final BookImageRepository bookImageRepository;
    private final RelatedBookRepository relatedBookRepository;
    private final InventoryRepository inventoryRepository;
    private final CategoryRepository categoryRepository;
    private final PublisherRepository publisherRepository;
    private final AuthorRepository authorRepository;
    private final VirtualEditionRepository virtualEditionRepository;
    private final ReviewRepository reviewRepository;
    private final CommerceClient commerceClient;
    private final ObjectMapper objectMapper;

    public CatalogService(
            BookRepository bookRepository,
            BookImageRepository bookImageRepository,
            RelatedBookRepository relatedBookRepository,
            InventoryRepository inventoryRepository,
            CategoryRepository categoryRepository,
            PublisherRepository publisherRepository,
            AuthorRepository authorRepository,
            VirtualEditionRepository virtualEditionRepository,
            ReviewRepository reviewRepository,
            CommerceClient commerceClient,
            ObjectMapper objectMapper
    ) {
        this.bookRepository = bookRepository;
        this.bookImageRepository = bookImageRepository;
        this.relatedBookRepository = relatedBookRepository;
        this.inventoryRepository = inventoryRepository;
        this.categoryRepository = categoryRepository;
        this.publisherRepository = publisherRepository;
        this.authorRepository = authorRepository;
        this.virtualEditionRepository = virtualEditionRepository;
        this.reviewRepository = reviewRepository;
        this.commerceClient = commerceClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Flattens the stored {@code {"Section": ["topic", ...], ...}} JSON into a single list of
     * topic names — just names for now, no nested section structure exposed to callers.
     */
    private List<String> extractTopics(String tableOfContentsJson) {
        if (tableOfContentsJson == null || tableOfContentsJson.isBlank()) {
            return List.of();
        }
        try {
            Map<String, List<String>> sections = objectMapper.readValue(
                    tableOfContentsJson, new TypeReference<Map<String, List<String>>>() {
                    });
            return sections.values().stream().flatMap(List::stream).toList();
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    /**
     * virtualOnly splits the catalogue into two independent browse paths: false (default,
     * "Physical" tab) shows books that have a store, i.e. real stock somewhere; true ("Virtual
     * editions" tab) shows books with an active virtual edition, ignoring store entirely — a
     * virtual edition is universally available to any customer regardless of which store they're
     * shopping. A book can appear in neither, either, or both tabs depending on what it has.
     */
    @Transactional(readOnly = true)
    public PageResponse<BookSummaryResponse> search(
            String query, UUID categoryId, UUID publisherId,
            BigDecimal minPrice, BigDecimal maxPrice, boolean virtualOnly, UUID storeId, Pageable pageable
    ) {
        Page<Book> page = bookRepository.findAll(
                BookSpecifications.withFilters(query, categoryId, publisherId, minPrice, maxPrice, virtualOnly, storeId),
                pageable
        );

        List<BookSummaryResponse> items = page.getContent().stream()
                .map(virtualOnly ? this::toVirtualSummary : this::toSummary)
                .toList();

        return new PageResponse<>(items, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    @Transactional(readOnly = true)
    public BookDetailResponse getDetail(UUID bookId) {
        Book book = bookRepository.findById(bookId)
                .filter(Book::isActive)
                .orElseThrow(BookNotFoundException::new);

        Inventory inventory = inventoryRepository.findById(bookId).orElse(null);
        int available = inventory != null ? inventory.getAvailable() : 0;

        // The gallery table supports multiple images per book, but is never populated in seed
        // data — every book's real image lives in the simpler coverImageUrl column instead (the
        // one every list/grid view already reads from). Fall back to it so the detail page's
        // hero image doesn't show a placeholder for a book that clearly has an image everywhere
        // else it's displayed.
        List<String> images = bookImageRepository.findAllByBookIdOrderBySortOrder(bookId).stream()
                .map(BookImage::getUrl)
                .toList();
        if (images.isEmpty() && book.getCoverImageUrl() != null) {
            images = List.of(book.getCoverImageUrl());
        }

        List<BookDetailResponse.AuthorRef> authors = book.getAuthors().stream()
                .map(a -> new BookDetailResponse.AuthorRef(a.getId(), a.getName(), a.getBio()))
                .toList();

        BookDetailResponse.CategoryRef category = book.getCategory() != null
                ? new BookDetailResponse.CategoryRef(book.getCategory().getId(), book.getCategory().getName())
                : null;

        BookDetailResponse.PublisherRef publisher = book.getPublisher() != null
                ? new BookDetailResponse.PublisherRef(book.getPublisher().getId(), book.getPublisher().getName())
                : null;

        BookDetailResponse.VirtualEditionRef virtualEdition = virtualEditionRepository.findById(bookId)
                .filter(VirtualEdition::isActive)
                .map(ve -> new BookDetailResponse.VirtualEditionRef(ve.getPrice(), ve.getCurrency()))
                .orElse(null);

        ReviewRepository.RatingAggregate rating = reviewRepository.getAggregateForBook(bookId);

        return new BookDetailResponse(
                book.getId(), book.getIsbn13(), book.getTitle(), book.getSubtitle(), book.getDescription(),
                authors, category, publisher, book.getPageCount(), book.getLanguage(),
                book.getPublishedOn(), book.getListPrice(), book.getCurrency(), images,
                new BookDetailResponse.Availability(available > 0 ? "IN_STOCK" : "OUT_OF_STOCK", available),
                3, virtualEdition, extractTopics(book.getTableOfContents()),
                rating.getAverageRating(), rating.getReviewCount()
        );
    }

    @Transactional(readOnly = true)
    public List<RelatedBookResponse> getRelated(UUID bookId) {
        if (!bookRepository.existsById(bookId)) {
            throw new BookNotFoundException();
        }

        return relatedBookRepository.findAllByBookId(bookId).stream()
                .map(RelatedBook::getRelatedBook)
                .map(b -> new RelatedBookResponse(b.getId(), b.getTitle(), b.getListPrice(), b.getCoverImageUrl()))
                .toList();
    }

    /** Flat list — categories are deliberately 1D, no nesting. */
    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategoryTree() {
        return categoryRepository.findAllByOrderByDisplayOrder().stream()
                .map(c -> new CategoryResponse(c.getId(), c.getName(), c.getSlug(), List.of()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PublisherResponse> getAllPublishers() {
        return publisherRepository.findAll().stream()
                .map(p -> new PublisherResponse(p.getId(), p.getName(), p.getSlug()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuthorResponse> getAllAuthors() {
        return authorRepository.findAll().stream()
                .map(a -> new AuthorResponse(a.getId(), a.getName(), a.getSlug(), a.getBio()))
                .toList();
    }

    /**
     * Recommends active books from the same categories as the caller's past purchases, excluding
     * titles they already own. Best-effort: if commerce-service is unreachable or the caller has
     * no purchase history, this returns an empty list rather than an error.
     */
    @Transactional(readOnly = true)
    public List<BookSummaryResponse> getRecommendations(UUID userId) {
        List<UUID> purchasedBookIds = commerceClient.getPurchasedBookIds(userId);
        if (purchasedBookIds.isEmpty()) {
            return List.of();
        }

        Set<UUID> categoryIds = bookRepository.findAllById(purchasedBookIds).stream()
                .map(Book::getCategory)
                .filter(java.util.Objects::nonNull)
                .map(Category::getId)
                .collect(Collectors.toSet());
        if (categoryIds.isEmpty()) {
            return List.of();
        }

        return bookRepository
                .findAllByCategory_IdInAndIdNotInAndIsActiveTrue(categoryIds, Set.copyOf(purchasedBookIds), PageRequest.of(0, 10))
                .stream()
                .map(this::toSummary)
                .toList();
    }

    /**
     * Backs the header search bar's typeahead — a plain case-insensitive title substring match,
     * same as the main search's query filter, just capped to a handful of results and returned
     * with a lighter payload. Deliberately not routed through ai-service's semantic search: that
     * endpoint embeds the query synchronously per call and sits behind a 20-req/min gateway rate
     * limit, both a poor fit for a call fired on every keystroke.
     */
    @Transactional(readOnly = true)
    public List<BookSuggestionResponse> suggest(String query, int limit) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        int cappedLimit = Math.min(Math.max(limit, 1), 10);
        return bookRepository
                .findAllByIsActiveTrueAndTitleContainingIgnoreCase(query, PageRequest.of(0, cappedLimit))
                .stream()
                .map(book -> new BookSuggestionResponse(
                        book.getId(), book.getTitle(), book.getAuthors().stream().map(Author::getName).toList(),
                        book.getListPrice(), book.getCurrency(), book.getCoverImageUrl()
                ))
                .toList();
    }

    private BookSummaryResponse toSummary(Book book) {
        Inventory inventory = inventoryRepository.findById(book.getId()).orElse(null);
        int available = inventory != null ? inventory.getAvailable() : 0;

        List<String> authorNames = book.getAuthors().stream().map(a -> a.getName()).toList();
        String publisherName = book.getPublisher() != null ? book.getPublisher().getName() : null;
        ReviewRepository.RatingAggregate rating = reviewRepository.getAggregateForBook(book.getId());

        return new BookSummaryResponse(
                book.getId(), book.getIsbn13(), book.getTitle(), authorNames, publisherName,
                book.getListPrice(), book.getCurrency(), book.getCoverImageUrl(),
                available > 0 ? "IN_STOCK" : "OUT_OF_STOCK", rating.getAverageRating(), rating.getReviewCount()
        );
    }

    /** Prices at the virtual edition's own price (can differ from the physical list price) — always "IN_STOCK": a digital copy doesn't deplete. */
    private BookSummaryResponse toVirtualSummary(Book book) {
        VirtualEdition virtualEdition = virtualEditionRepository.findById(book.getId()).orElse(null);

        List<String> authorNames = book.getAuthors().stream().map(a -> a.getName()).toList();
        String publisherName = book.getPublisher() != null ? book.getPublisher().getName() : null;
        BigDecimal price = virtualEdition != null ? virtualEdition.getPrice() : book.getListPrice();
        String currency = virtualEdition != null ? virtualEdition.getCurrency() : book.getCurrency();
        ReviewRepository.RatingAggregate rating = reviewRepository.getAggregateForBook(book.getId());

        return new BookSummaryResponse(
                book.getId(), book.getIsbn13(), book.getTitle(), authorNames, publisherName,
                price, currency, book.getCoverImageUrl(), "IN_STOCK", rating.getAverageRating(), rating.getReviewCount()
        );
    }
}
