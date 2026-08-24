package com.readora.catalog.service;

import com.readora.catalog.dto.BookExportItem;
import com.readora.catalog.dto.BookExportPage;
import com.readora.catalog.dto.BookLookupResponse;
import com.readora.catalog.dto.VirtualEditionLookupResponse;
import com.readora.catalog.entity.Author;
import com.readora.catalog.entity.Book;
import com.readora.catalog.entity.VirtualEdition;
import com.readora.catalog.repository.BookRepository;
import com.readora.catalog.repository.VirtualEditionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public InternalCatalogService(BookRepository bookRepository, VirtualEditionRepository virtualEditionRepository) {
        this.bookRepository = bookRepository;
        this.virtualEditionRepository = virtualEditionRepository;
    }

    /** Full text export for ai-service's embedding backfill — title, authors, description, table of contents. */
    @Transactional(readOnly = true)
    public BookExportPage exportBooks(Pageable pageable) {
        Page<Book> page = bookRepository.findAll(pageable);

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
}
