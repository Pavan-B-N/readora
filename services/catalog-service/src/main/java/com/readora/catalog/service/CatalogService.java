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
import com.readora.catalog.dto.PurchasedBookResponse;
import com.readora.catalog.dto.RecentOrderItemResponse;
import com.readora.catalog.dto.RelatedBookResponse;
import com.readora.catalog.entity.Author;
import com.readora.catalog.entity.Book;
import com.readora.catalog.entity.BookImage;
import com.readora.catalog.entity.Category;
import com.readora.catalog.entity.Inventory;
import com.readora.catalog.entity.RelatedBook;
import com.readora.catalog.entity.VirtualEdition;
import com.readora.catalog.exception.BookNotFoundException;
import com.readora.catalog.exception.StoreIdRequiredException;
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
     * virtualOnly is nullable and three-valued: TRUE returns only books with an active virtual
     * edition (store-independent); FALSE returns only physical books at storeId; null (the
     * default, single unified catalogue — the storefront doesn't split physical/virtual into
     * separate tabs) returns everything available to this customer, physical-at-their-store or
     * virtual-anywhere, the same "available to customer" rule used elsewhere in this class.
     * storeId is required whenever a physical result could appear (virtualOnly FALSE or null) —
     * a customer only ever sees stock from the one store they're delivering from, never a
     * cross-store view — and is rejected rather than silently ignored when missing, so a caller
     * can't accidentally see an unscoped result set. userId (null for anonymous callers) is used
     * only to hide virtual-only books the caller already owns — see BookSpecifications.withFilters.
     */
    @Transactional(readOnly = true)
    public PageResponse<BookSummaryResponse> search(
            String query, UUID categoryId, UUID publisherId,
            BigDecimal minPrice, BigDecimal maxPrice, Boolean virtualOnly, UUID storeId, UUID userId, Pageable pageable
    ) {
        if (!Boolean.TRUE.equals(virtualOnly) && storeId == null) {
            throw new StoreIdRequiredException();
        }

        Set<UUID> ownedBookIds = userId != null
                ? Set.copyOf(commerceClient.getPurchasedBookIds(userId))
                : Set.of();

        Page<Book> page = bookRepository.findAll(
                BookSpecifications.withFilters(query, categoryId, publisherId, minPrice, maxPrice, virtualOnly, storeId, ownedBookIds),
                pageable
        );

        List<BookSummaryResponse> items = page.getContent().stream()
                .map(book -> book.getStore() == null ? toVirtualSummary(book) : toSummary(book))
                .toList();

        return new PageResponse<>(items, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    /**
     * Backs the "Your orders" rail — the caller's most recent order line items (newest first,
     * every status included, cancelled/returned too) paired with book display data. Not filtered
     * by store: an order already placed is a fact of history regardless of where the caller is
     * shopping from now. One entry per order item, so the same book can appear more than once if
     * it was ordered in separate orders — each with its own status.
     */
    @Transactional(readOnly = true)
    public List<PurchasedBookResponse> getPurchasedBooks(UUID userId) {
        List<RecentOrderItemResponse> items = commerceClient.getRecentOrderItems(userId, 20);
        if (items.isEmpty()) {
            return List.of();
        }

        Map<UUID, Book> booksById = bookRepository.findAllById(items.stream().map(RecentOrderItemResponse::bookId).toList())
                .stream()
                .collect(Collectors.toMap(Book::getId, book -> book));

        return items.stream()
                .map(item -> {
                    Book book = booksById.get(item.bookId());
                    if (book == null) return null;
                    BookSummaryResponse summary = book.getStore() == null ? toVirtualSummary(book) : toSummary(book);
                    return new PurchasedBookResponse(summary, item.status(), item.placedAt());
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    /** Arbitrary book lookup by id, e.g. to render a wishlist — deliberately unscoped by store, since a saved-for-later list should still show what you saved even if it's not stocked at your current store. */
    @Transactional(readOnly = true)
    public List<BookSummaryResponse> getBooksByIds(List<UUID> bookIds) {
        if (bookIds.isEmpty()) {
            return List.of();
        }
        return bookRepository.findAllById(bookIds).stream()
                .filter(Book::isActive)
                .map(book -> book.getStore() == null ? toVirtualSummary(book) : toSummary(book))
                .toList();
    }

    /**
     * storeId is the caller's currently-delivering-from store. A physical book stocked at a
     * different store is never purchasable by this caller — regardless of its own inventory
     * count, availability is reported as NOT_AVAILABLE_AT_STORE so the frontend can't offer
     * "Add to cart" for stock the caller could never actually receive. Virtual-only books
     * (book.getStore() == null) are unaffected — a virtual edition is store-independent.
     * storeId may be null (e.g. an anonymous caller whose store hasn't resolved yet); in that
     * case physical availability falls back to the plain inventory count.
     */
    @Transactional(readOnly = true)
    public BookDetailResponse getDetail(UUID bookId, UUID storeId) {
        Book book = bookRepository.findById(bookId)
                .filter(Book::isActive)
                .orElseThrow(BookNotFoundException::new);

        Inventory inventory = inventoryRepository.findById(bookId).orElse(null);
        int available = inventory != null ? inventory.getAvailable() : 0;
        boolean wrongStore = book.getStore() != null && storeId != null && !book.getStore().getId().equals(storeId);

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
                .map(a -> new BookDetailResponse.AuthorRef(a.getId(), a.getName(), a.getBio(), a.getPhotoUrl()))
                .toList();

        BookDetailResponse.CategoryRef category = book.getCategory() != null
                ? new BookDetailResponse.CategoryRef(book.getCategory().getId(), book.getCategory().getName())
                : null;

        BookDetailResponse.PublisherRef publisher = book.getPublisher() != null
                ? new BookDetailResponse.PublisherRef(book.getPublisher().getId(), book.getPublisher().getName())
                : null;

        BookDetailResponse.StoreRef store = book.getStore() != null
                ? new BookDetailResponse.StoreRef(book.getStore().getId(), book.getStore().getName(), book.getStore().getCity())
                : null;

        BookDetailResponse.VirtualEditionRef virtualEdition = virtualEditionRepository.findById(bookId)
                .filter(VirtualEdition::isActive)
                .map(ve -> new BookDetailResponse.VirtualEditionRef(ve.getPrice(), ve.getCurrency()))
                .orElse(null);

        ReviewRepository.RatingAggregate rating = reviewRepository.getAggregateForBook(bookId);

        // A virtual-only book (no store at all) has no physical-stock concept to report — it's
        // never "out of stock," that status is reserved for a physical book genuinely sold out at
        // the customer's own store. NO_PHYSICAL_EDITION lets the frontend skip the discouraging
        // "out of stock" messaging for a book that's actually fully purchasable, just not physically.
        String availabilityStatus = book.getStore() == null
                ? "NO_PHYSICAL_EDITION"
                : wrongStore ? "NOT_AVAILABLE_AT_STORE" : (available > 0 ? "IN_STOCK" : "OUT_OF_STOCK");

        return new BookDetailResponse(
                book.getId(), book.getIsbn13(), book.getTitle(), book.getSubtitle(), book.getDescription(),
                authors, category, publisher, store, book.getPageCount(), book.getLanguage(),
                book.getPublishedOn(), book.getListPrice(), book.getCurrency(), images,
                new BookDetailResponse.Availability(availabilityStatus, wrongStore ? 0 : available),
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
                .map(c -> new CategoryResponse(c.getId(), c.getName(), c.getSlug(), c.getDisplayOrder(), List.of()))
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
                .map(a -> new AuthorResponse(a.getId(), a.getName(), a.getSlug(), a.getBio(), a.getPhotoUrl()))
                .toList();
    }

    /**
     * Recommends active books from the same categories as the caller's past purchases, excluding
     * titles they already own. Best-effort: if commerce-service is unreachable or the caller has
     * no purchase history, this returns an empty list rather than an error.
     */
    @Transactional(readOnly = true)
    public List<BookSummaryResponse> getRecommendations(UUID userId, UUID storeId) {
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
                .findAll(
                        BookSpecifications.forRecommendations(categoryIds, Set.copyOf(purchasedBookIds), storeId),
                        PageRequest.of(0, 10)
                )
                .getContent()
                .stream()
                .map(book -> book.getStore() == null ? toVirtualSummary(book) : toSummary(book))
                .toList();
    }

    /**
     * Backs the header search bar's typeahead — a plain case-insensitive title substring match,
     * same as the main search's query filter, just capped to a handful of results and returned
     * with a lighter payload. Deliberately not routed through ai-service's semantic search: that
     * endpoint embeds the query synchronously per call and sits behind a 20-req/min gateway rate
     * limit, both a poor fit for a call fired on every keystroke. Scoped to what's available to
     * this customer (their store, or store-independent virtual editions) so a suggestion is never
     * a book they then can't actually find in the physical/virtual search results.
     */
    @Transactional(readOnly = true)
    public List<BookSuggestionResponse> suggest(String query, int limit, UUID storeId) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        int cappedLimit = Math.min(Math.max(limit, 1), 10);
        return bookRepository
                .findAll(BookSpecifications.forSuggest(query, storeId), PageRequest.of(0, cappedLimit))
                .getContent()
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
        String categoryName = book.getCategory() != null ? book.getCategory().getName() : null;
        ReviewRepository.RatingAggregate rating = reviewRepository.getAggregateForBook(book.getId());
        boolean hasVirtualEdition = virtualEditionRepository.findById(book.getId()).filter(VirtualEdition::isActive).isPresent();

        return new BookSummaryResponse(
                book.getId(), book.getIsbn13(), book.getTitle(), authorNames, publisherName, categoryName,
                book.getListPrice(), book.getCurrency(), book.getCoverImageUrl(),
                available > 0 ? "IN_STOCK" : "OUT_OF_STOCK", hasVirtualEdition, "PHYSICAL", rating.getAverageRating(), rating.getReviewCount()
        );
    }

    /** Prices at the virtual edition's own price (can differ from the physical list price) — always "IN_STOCK": a digital copy doesn't deplete. */
    private BookSummaryResponse toVirtualSummary(Book book) {
        VirtualEdition virtualEdition = virtualEditionRepository.findById(book.getId()).filter(VirtualEdition::isActive).orElse(null);

        List<String> authorNames = book.getAuthors().stream().map(a -> a.getName()).toList();
        String publisherName = book.getPublisher() != null ? book.getPublisher().getName() : null;
        String categoryName = book.getCategory() != null ? book.getCategory().getName() : null;
        BigDecimal price = virtualEdition != null ? virtualEdition.getPrice() : book.getListPrice();
        String currency = virtualEdition != null ? virtualEdition.getCurrency() : book.getCurrency();
        ReviewRepository.RatingAggregate rating = reviewRepository.getAggregateForBook(book.getId());

        return new BookSummaryResponse(
                book.getId(), book.getIsbn13(), book.getTitle(), authorNames, publisherName, categoryName,
                price, currency, book.getCoverImageUrl(), "IN_STOCK", true, "VIRTUAL", rating.getAverageRating(), rating.getReviewCount()
        );
    }
}
