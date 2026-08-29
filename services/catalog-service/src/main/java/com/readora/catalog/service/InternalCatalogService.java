package com.readora.catalog.service;

import com.readora.catalog.dto.BookAvailabilityResponse;
import com.readora.catalog.dto.BookCoverLookupResponse;
import com.readora.catalog.dto.BookExportItem;
import com.readora.catalog.dto.BookExportPage;
import com.readora.catalog.dto.BookLookupResponse;
import com.readora.catalog.dto.StoreResponse;
import com.readora.catalog.dto.VirtualEditionLookupResponse;
import com.readora.catalog.entity.Author;
import com.readora.catalog.entity.Book;
import com.readora.catalog.entity.Inventory;
import com.readora.catalog.entity.Store;
import com.readora.catalog.entity.VirtualEdition;
import com.readora.catalog.exception.StoreNotFoundException;
import com.readora.catalog.repository.BookRepository;
import com.readora.catalog.repository.InventoryRepository;
import com.readora.catalog.repository.StoreRepository;
import com.readora.catalog.repository.VirtualEditionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service-to-service only — backs the /internal endpoints ai-service and commerce-service call
 * directly (gateway-secret protected, not part of the public API surface).
 */
@Service
public class InternalCatalogService {

    private final BookRepository bookRepository;
    private final VirtualEditionRepository virtualEditionRepository;
    private final InventoryRepository inventoryRepository;
    private final StoreRepository storeRepository;

    public InternalCatalogService(
            BookRepository bookRepository,
            VirtualEditionRepository virtualEditionRepository,
            InventoryRepository inventoryRepository,
            StoreRepository storeRepository
    ) {
        this.bookRepository = bookRepository;
        this.virtualEditionRepository = virtualEditionRepository;
        this.inventoryRepository = inventoryRepository;
        this.storeRepository = storeRepository;
    }

    /**
     * Looked up by commerce-service during checkout to validate a shipping address's city
     * actually matches the store the order is delivering from — commerce-service holds no local
     * store data of its own.
     */
    @Transactional(readOnly = true)
    public StoreResponse findStore(UUID storeId) {
        Store store = storeRepository.findById(storeId).orElseThrow(StoreNotFoundException::new);
        return new StoreResponse(
                store.getId(), store.getName(), store.getCity(), store.getLine1(), store.getLine2(),
                store.getState(), store.getPostalCode(), store.getCountryCode()
        );
    }

    /**
     * Full text export for ai-service's embedding backfill — title, authors, description, table
     * of contents. When {@code needsReembeddingOnly} is set, only books whose content changed
     * since they were last embedded (or that have never been embedded) are returned, so a repeat
     * backfill run doesn't re-embed the whole catalog every time.
     */
    @Transactional(readOnly = true)
    public BookExportPage exportBooks(Pageable pageable, boolean needsReembeddingOnly) {
        Page<Book> page = needsReembeddingOnly
                ? bookRepository.findNeedingReembedding(pageable)
                : bookRepository.findAll(pageable);

        List<BookExportItem> items = page.getContent().stream()
                .map(book -> new BookExportItem(
                        book.getId(),
                        book.getTitle(),
                        book.getAuthors().stream().map(Author::getName).toList(),
                        book.getDescription(),
                        book.getTableOfContents()
                ))
                .toList();

        return new BookExportPage(items, page.getTotalPages());
    }

    /** Marks the given books as embedded as of now — called by ai-service after it successfully embeds them. */
    @Transactional
    public void markEmbedded(List<UUID> bookIds) {
        if (bookIds.isEmpty()) return;
        bookRepository.markEmbedded(bookIds, Instant.now());
    }

    /** Text export for the specific book ids requested — used by ai-service's incremental embedding consumer, so it doesn't have to paginate the whole catalog for one changed book. */
    @Transactional(readOnly = true)
    public BookLookupResponse lookupBooks(List<UUID> bookIds) {
        List<BookExportItem> items = bookRepository.findAllById(bookIds).stream()
                .map(book -> new BookExportItem(
                        book.getId(),
                        book.getTitle(),
                        book.getAuthors().stream().map(Author::getName).toList(),
                        book.getDescription(),
                        book.getTableOfContents()
                ))
                .toList();

        return new BookLookupResponse(items);
    }

    /** Cover image lookup for the given book ids — used by commerce-service to render order-list thumbnails. */
    @Transactional(readOnly = true)
    public BookCoverLookupResponse lookupCovers(List<UUID> bookIds) {
        List<BookCoverLookupResponse.Item> items = bookRepository.findAllById(bookIds).stream()
                .map(book -> new BookCoverLookupResponse.Item(book.getId(), book.getCoverImageUrl()))
                .toList();

        return new BookCoverLookupResponse(items);
    }

    /** Virtual-edition availability and pricing lookup for commerce-service's virtual checkout path. */
    @Transactional(readOnly = true)
    public VirtualEditionLookupResponse lookupVirtualEditions(List<UUID> bookIds) {
        List<VirtualEditionLookupResponse.Item> items = bookIds.stream()
                .map(bookId -> {
                    Optional<Book> book = bookRepository.findById(bookId).filter(Book::isActive);
                    Optional<VirtualEdition> edition = virtualEditionRepository.findById(bookId).filter(VirtualEdition::isActive);

                    boolean available = book.isPresent() && edition.isPresent();

                    return new VirtualEditionLookupResponse.Item(
                            bookId,
                            book.map(Book::getTitle).orElse(null),
                            available,
                            edition.map(VirtualEdition::getPrice).orElse(null),
                            edition.map(VirtualEdition::getCurrency).orElse(null)
                    );
                })
                .toList();

        return new VirtualEditionLookupResponse(items);
    }

    /**
     * Filters a candidate list down to what's actually purchasable right now — used by ai-service's
     * book-recommendation tools so the assistant can never suggest a title the caller has no way to
     * buy: one with a virtual edition is available regardless of store, and a physical-only book is
     * available only when its (single, fixed — see the book/store model this class already assumes
     * elsewhere) home store matches the caller's storeId and it actually has stock there.
     */
    @Transactional(readOnly = true)
    public BookAvailabilityResponse checkAvailability(List<UUID> bookIds, UUID storeId) {
        List<UUID> available = bookIds.stream().filter(id -> isAvailable(id, storeId)).toList();
        return new BookAvailabilityResponse(available);
    }

    private boolean isAvailable(UUID bookId, UUID storeId) {
        Optional<Book> book = bookRepository.findById(bookId).filter(Book::isActive);
        if (book.isEmpty()) {
            return false;
        }

        boolean hasVirtualEdition = virtualEditionRepository.findById(bookId).filter(VirtualEdition::isActive).isPresent();
        if (hasVirtualEdition) {
            return true;
        }

        boolean rightStore = book.get().getStore() != null
                && storeId != null
                && book.get().getStore().getId().equals(storeId);
        if (!rightStore) {
            return false;
        }

        Inventory inventory = inventoryRepository.findById(bookId).orElse(null);
        return inventory != null && inventory.getAvailable() > 0;
    }
}
