package com.readora.catalog.service;

import com.readora.catalog.client.CommerceClient;
import com.readora.catalog.dto.AuthorResponse;
import com.readora.catalog.dto.BookDetailResponse;
import com.readora.catalog.dto.BookSummaryResponse;
import com.readora.catalog.dto.CategoryResponse;
import com.readora.catalog.dto.PageResponse;
import com.readora.catalog.dto.PublisherResponse;
import com.readora.catalog.dto.RelatedBookResponse;
import com.readora.catalog.entity.Book;
import com.readora.catalog.entity.BookFormat;
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
import com.readora.catalog.repository.VirtualEditionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
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
    private final CommerceClient commerceClient;

    public CatalogService(
            BookRepository bookRepository,
            BookImageRepository bookImageRepository,
            RelatedBookRepository relatedBookRepository,
            InventoryRepository inventoryRepository,
            CategoryRepository categoryRepository,
            PublisherRepository publisherRepository,
            AuthorRepository authorRepository,
            VirtualEditionRepository virtualEditionRepository,
            CommerceClient commerceClient
    ) {
        this.bookRepository = bookRepository;
        this.bookImageRepository = bookImageRepository;
        this.relatedBookRepository = relatedBookRepository;
        this.inventoryRepository = inventoryRepository;
        this.categoryRepository = categoryRepository;
        this.publisherRepository = publisherRepository;
        this.authorRepository = authorRepository;
        this.virtualEditionRepository = virtualEditionRepository;
        this.commerceClient = commerceClient;
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
            String query, UUID categoryId, UUID publisherId, BookFormat format,
            BigDecimal minPrice, BigDecimal maxPrice, boolean virtualOnly, Pageable pageable
    ) {
        Page<Book> page = bookRepository.findAll(
                BookSpecifications.withFilters(query, categoryId, publisherId, format, minPrice, maxPrice, virtualOnly),
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

        List<String> images = bookImageRepository.findAllByBookIdOrderBySortOrder(bookId).stream()
                .map(BookImage::getUrl)
                .toList();

        List<BookDetailResponse.AuthorRef> authors = book.getAuthors().stream()
                .map(a -> new BookDetailResponse.AuthorRef(a.getId(), a.getName()))
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

        return new BookDetailResponse(
                book.getId(), book.getIsbn13(), book.getTitle(), book.getSubtitle(), book.getDescription(),
                authors, category, publisher, book.getFormat().name(), book.getPageCount(), book.getLanguage(),
                book.getPublishedOn(), book.getListPrice(), book.getCurrency(), images,
                new BookDetailResponse.Availability(available > 0 ? "IN_STOCK" : "OUT_OF_STOCK", available),
                3, virtualEdition
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

    private BookSummaryResponse toSummary(Book book) {
        Inventory inventory = inventoryRepository.findById(book.getId()).orElse(null);
        int available = inventory != null ? inventory.getAvailable() : 0;

        List<String> authorNames = book.getAuthors().stream().map(a -> a.getName()).toList();
        String publisherName = book.getPublisher() != null ? book.getPublisher().getName() : null;

        return new BookSummaryResponse(
                book.getId(), book.getIsbn13(), book.getTitle(), authorNames, publisherName,
                book.getFormat().name(), book.getListPrice(), book.getCurrency(), book.getCoverImageUrl(),
                available > 0 ? "IN_STOCK" : "OUT_OF_STOCK"
        );
    }

    /** Prices at the virtual edition's own price (can differ from the physical list price) — always "IN_STOCK": a digital copy doesn't deplete. */
    private BookSummaryResponse toVirtualSummary(Book book) {
        VirtualEdition virtualEdition = virtualEditionRepository.findById(book.getId()).orElse(null);

        List<String> authorNames = book.getAuthors().stream().map(a -> a.getName()).toList();
        String publisherName = book.getPublisher() != null ? book.getPublisher().getName() : null;
        BigDecimal price = virtualEdition != null ? virtualEdition.getPrice() : book.getListPrice();
        String currency = virtualEdition != null ? virtualEdition.getCurrency() : book.getCurrency();

        return new BookSummaryResponse(
                book.getId(), book.getIsbn13(), book.getTitle(), authorNames, publisherName,
                book.getFormat().name(), price, currency, book.getCoverImageUrl(), "IN_STOCK"
        );
    }
}
