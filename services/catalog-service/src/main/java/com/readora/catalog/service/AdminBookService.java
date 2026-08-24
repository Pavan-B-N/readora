package com.readora.catalog.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.readora.catalog.dto.AdminBookDetailResponse;
import com.readora.catalog.dto.BookUpsertedEvent;
import com.readora.catalog.dto.CreateBookRequest;
import com.readora.catalog.dto.IdResponse;
import com.readora.catalog.dto.UpdateBookRequest;
import com.readora.catalog.dto.UpdateInventoryRequest;
import com.readora.catalog.dto.UpsertVirtualEditionRequest;
import com.readora.catalog.entity.Author;
import com.readora.catalog.entity.Book;
import com.readora.catalog.entity.Category;
import com.readora.catalog.entity.Inventory;
import com.readora.catalog.entity.OutboxEvent;
import com.readora.catalog.entity.Publisher;
import com.readora.catalog.entity.VirtualEdition;
import com.readora.catalog.exception.AuthorNotFoundException;
import com.readora.catalog.exception.BookNotFoundException;
import com.readora.catalog.exception.CategoryNotFoundException;
import com.readora.catalog.exception.PublisherNotFoundException;
import com.readora.catalog.kafka.KafkaTopics;
import com.readora.catalog.repository.AuthorRepository;
import com.readora.catalog.repository.BookRepository;
import com.readora.catalog.repository.CategoryRepository;
import com.readora.catalog.repository.InventoryRepository;
import com.readora.catalog.repository.OutboxEventRepository;
import com.readora.catalog.repository.PublisherRepository;
import com.readora.catalog.repository.VirtualEditionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Admin creation/update of books, their stock levels, and their virtual editions. */
@Service
public class AdminBookService {

    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;
    private final PublisherRepository publisherRepository;
    private final AuthorRepository authorRepository;
    private final InventoryRepository inventoryRepository;
    private final VirtualEditionRepository virtualEditionRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public AdminBookService(
            BookRepository bookRepository,
            CategoryRepository categoryRepository,
            PublisherRepository publisherRepository,
            AuthorRepository authorRepository,
            InventoryRepository inventoryRepository,
            VirtualEditionRepository virtualEditionRepository,
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper
    ) {
        this.bookRepository = bookRepository;
        this.categoryRepository = categoryRepository;
        this.publisherRepository = publisherRepository;
        this.authorRepository = authorRepository;
        this.inventoryRepository = inventoryRepository;
        this.virtualEditionRepository = virtualEditionRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public AdminBookDetailResponse getBookForEdit(UUID bookId) {
        Book book = bookRepository.findById(bookId).orElseThrow(BookNotFoundException::new);
        Inventory inventory = inventoryRepository.findById(bookId).orElse(null);
        VirtualEdition virtualEdition = virtualEditionRepository.findById(bookId).orElse(null);

        AdminBookDetailResponse.Inventory inventoryDto = inventory != null
                ? new AdminBookDetailResponse.Inventory(inventory.getQtyOnHand(), inventory.getQtyReserved(), inventory.getReorderThreshold())
                : null;

        AdminBookDetailResponse.VirtualEdition virtualEditionDto = virtualEdition != null
                ? new AdminBookDetailResponse.VirtualEdition(
                        virtualEdition.getFileUrl(), virtualEdition.getFileFormat(), virtualEdition.getFileSizeBytes(),
                        virtualEdition.getPrice(), virtualEdition.getCurrency(), virtualEdition.isActive()
                )
                : null;

        return new AdminBookDetailResponse(
                book.getId(), book.getIsbn13(), book.getTitle(), book.getSubtitle(), book.getDescription(),
                book.getTableOfContents(),
                book.getCategory() != null ? book.getCategory().getId() : null,
                book.getPublisher() != null ? book.getPublisher().getId() : null,
                book.getAuthors().stream().map(Author::getId).toList(),
                book.getLanguage(), book.getFormat(), book.getPageCount(), book.getPublishedOn(),
                book.getListPrice(), book.getCurrency(), book.getCoverImageUrl(), book.isActive(),
                inventoryDto, virtualEditionDto
        );
    }

    @Transactional
    public IdResponse createBook(CreateBookRequest request) {
        Category category = request.categoryId() != null
                ? categoryRepository.findById(request.categoryId()).orElseThrow(CategoryNotFoundException::new)
                : null;
        Publisher publisher = request.publisherId() != null
                ? publisherRepository.findById(request.publisherId()).orElseThrow(PublisherNotFoundException::new)
                : null;

        Book book = new Book(
                request.isbn13(), request.title(), request.subtitle(), request.description(), category,
                publisher, request.language(), request.format(), request.pageCount(), request.publishedOn(),
                request.listPrice(), request.currency(), request.coverImageUrl()
        );
        book.setTableOfContents(request.tableOfContents());

        for (UUID authorId : request.authorIds()) {
            Author author = authorRepository.findById(authorId).orElseThrow(AuthorNotFoundException::new);
            book.addAuthor(author);
        }

        bookRepository.save(book);
        publishBookUpserted(book.getId());
        return new IdResponse(book.getId());
    }

    @Transactional
    public void updateBook(UUID bookId, UpdateBookRequest request) {
        Book book = bookRepository.findById(bookId).orElseThrow(BookNotFoundException::new);

        Category category = request.categoryId() != null
                ? categoryRepository.findById(request.categoryId()).orElseThrow(CategoryNotFoundException::new)
                : null;
        Publisher publisher = request.publisherId() != null
                ? publisherRepository.findById(request.publisherId()).orElseThrow(PublisherNotFoundException::new)
                : null;

        book.update(
                request.title(), request.subtitle(), request.description(), request.tableOfContents(),
                category, publisher, request.language(), request.format(), request.pageCount(),
                request.publishedOn(), request.listPrice(), request.currency(), request.coverImageUrl(),
                request.isActive()
        );

        if (request.authorIds() != null) {
            List<UUID> authorIds = request.authorIds();
            Set<Author> authors = authorIds.stream()
                    .map(id -> authorRepository.findById(id).orElseThrow(AuthorNotFoundException::new))
                    .collect(Collectors.toSet());
            book.replaceAuthors(authors);
        }

        bookRepository.save(book);
        publishBookUpserted(book.getId());
    }

    private void publishBookUpserted(UUID bookId) {
        try {
            String payload = objectMapper.writeValueAsString(new BookUpsertedEvent(bookId));
            outboxEventRepository.save(new OutboxEvent("Book", bookId, KafkaTopics.BOOK_UPSERTED, payload));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize outbox event payload", e);
        }
    }

    @Transactional
    public void updateInventory(UUID bookId, UpdateInventoryRequest request) {
        Book book = bookRepository.findById(bookId).orElseThrow(BookNotFoundException::new);

        Inventory inventory = inventoryRepository.findById(bookId)
                .orElseGet(() -> new Inventory(book, 0, 0));

        inventory.restock(request.qtyOnHand(), request.reorderThreshold());
        inventoryRepository.save(inventory);
    }

    @Transactional
    public void upsertVirtualEdition(UUID bookId, UpsertVirtualEditionRequest request) {
        VirtualEdition edition = virtualEditionRepository.findById(bookId).orElse(null);

        if (edition != null) {
            edition.update(request.fileUrl(), request.fileFormat(), request.fileSizeBytes(), request.price(), request.currency());
        } else {
            Book book = bookRepository.findById(bookId).orElseThrow(BookNotFoundException::new);
            edition = new VirtualEdition(
                    book, request.fileUrl(), request.fileFormat(), request.fileSizeBytes(),
                    request.price(), request.currency()
            );
        }

        virtualEditionRepository.save(edition);
    }

    @Transactional
    public void deactivateVirtualEdition(UUID bookId) {
        VirtualEdition edition = virtualEditionRepository.findById(bookId).orElse(null);
        if (edition != null) {
            edition.deactivate();
            virtualEditionRepository.save(edition);
        }
    }
}
