package com.readora.catalog.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.readora.catalog.client.UserServiceClient;
import com.readora.catalog.dto.AdminBookDetailResponse;
import com.readora.sharedcore.event.BookUpsertedEvent;
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
import com.readora.catalog.entity.Store;
import com.readora.catalog.entity.VirtualEdition;
import com.readora.catalog.exception.AdminStoreAccessDeniedException;
import com.readora.catalog.exception.AdminStoreNotAssignedException;
import com.readora.catalog.exception.AuthorNotFoundException;
import com.readora.catalog.exception.BookNotFoundException;
import com.readora.catalog.exception.CategoryNotFoundException;
import com.readora.catalog.exception.IsbnAlreadyExistsException;
import com.readora.catalog.exception.PublisherNotFoundException;
import com.readora.catalog.exception.StoreNotFoundException;
import com.readora.catalog.kafka.KafkaTopics;
import com.readora.catalog.repository.AuthorRepository;
import com.readora.catalog.repository.BookRepository;
import com.readora.catalog.repository.CategoryRepository;
import com.readora.catalog.repository.InventoryRepository;
import com.readora.catalog.repository.OutboxEventRepository;
import com.readora.catalog.repository.PublisherRepository;
import com.readora.catalog.repository.StoreRepository;
import com.readora.catalog.repository.VirtualEditionRepository;
import com.readora.sharedcore.security.CurrentUserContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Admin creation/update of books, their stock levels, and their virtual editions. */
@Service
public class AdminBookService {

    private static final Logger log = LoggerFactory.getLogger(AdminBookService.class);
    private static final HttpClient FILE_SIZE_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;
    private final PublisherRepository publisherRepository;
    private final AuthorRepository authorRepository;
    private final StoreRepository storeRepository;
    private final InventoryRepository inventoryRepository;
    private final VirtualEditionRepository virtualEditionRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final UserServiceClient userServiceClient;

    // Wires in all repositories, Jackson, and the user-service client this service depends on.
    public AdminBookService(
            BookRepository bookRepository,
            CategoryRepository categoryRepository,
            PublisherRepository publisherRepository,
            AuthorRepository authorRepository,
            StoreRepository storeRepository,
            InventoryRepository inventoryRepository,
            VirtualEditionRepository virtualEditionRepository,
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper,
            UserServiceClient userServiceClient
    ) {
        this.bookRepository = bookRepository;
        this.categoryRepository = categoryRepository;
        this.publisherRepository = publisherRepository;
        this.authorRepository = authorRepository;
        this.storeRepository = storeRepository;
        this.inventoryRepository = inventoryRepository;
        this.virtualEditionRepository = virtualEditionRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
        this.userServiceClient = userServiceClient;
    }

    // Resolves which store the caller may manage books under — always derived server-side from the caller's own assignment, never a client-supplied storeId.
    private UUID resolveCallerStoreId() {
        UUID storeId = userServiceClient.getAdminStoreId(CurrentUserContext.require());
        if (storeId == null) {
            throw new AdminStoreNotAssignedException();
        }
        return storeId;
    }

    // Loads a book and enforces store-scoping (a store-stocked book is only manageable by its assigned admin; a storeless/virtual book is open to any admin); 404s rather than 403s on a mismatch, matching commerce-service's findByIdAndUserId convention — it simply doesn't exist as far as this admin is concerned.
    private Book requireManageableBook(UUID bookId, UUID callerStoreId) {
        Book book = bookRepository.findById(bookId).orElseThrow(BookNotFoundException::new);
        if (book.getStore() != null && !book.getStore().getId().equals(callerStoreId)) {
            throw new BookNotFoundException();
        }
        return book;
    }

    // Returns full editable detail (including inventory and virtual-edition state) for the admin edit form.
    @Transactional(readOnly = true)
    public AdminBookDetailResponse getBookForEdit(UUID bookId) {
        Book book = requireManageableBook(bookId, resolveCallerStoreId());
        Inventory inventory = inventoryRepository.findById(bookId).orElse(null);
        VirtualEdition virtualEdition = virtualEditionRepository.findById(bookId).orElse(null);

        AdminBookDetailResponse.Inventory inventoryDto = inventory != null
                ? new AdminBookDetailResponse.Inventory(inventory.getQtyOnHand(), inventory.getQtyReserved(), inventory.getReorderThreshold())
                : null;

        AdminBookDetailResponse.VirtualEdition virtualEditionDto = virtualEdition != null
                ? new AdminBookDetailResponse.VirtualEdition(
                        virtualEdition.getFileUrl(), virtualEdition.getFileFormat(), virtualEdition.getFileSizeBytes(),
                        virtualEdition.getPrice(), virtualEdition.getCurrency(), virtualEdition.isActive(),
                        virtualEdition.getCreatedByUserId()
                )
                : null;

        return new AdminBookDetailResponse(
                book.getId(), book.getIsbn13(), book.getTitle(), book.getDescription(),
                book.getTableOfContents(),
                book.getCategory() != null ? book.getCategory().getId() : null,
                book.getPublisher() != null ? book.getPublisher().getId() : null,
                book.getStore() != null ? book.getStore().getId() : null,
                book.getAuthors().stream().map(Author::getId).toList(),
                book.getLanguage(), book.getPageCount(), book.getPublishedOn(),
                book.getListPrice(), book.getCurrency(), book.getCoverImageUrl(), book.isActive(),
                book.getCreatedByUserId(), book.getCreatedAt(), book.getEmbeddedAt(), book.needsReembedding(),
                inventoryDto, virtualEditionDto
        );
    }

    // Creates a new book after validating ISBN uniqueness, category/publisher/store existence, and authors.
    @Transactional
    public IdResponse createBook(CreateBookRequest request) {
        if (bookRepository.existsByIsbn13(request.isbn13())) {
            throw new IsbnAlreadyExistsException(request.isbn13());
        }

        Category category = request.categoryId() != null
                ? categoryRepository.findById(request.categoryId()).orElseThrow(CategoryNotFoundException::new)
                : null;
        Publisher publisher = request.publisherId() != null
                ? publisherRepository.findById(request.publisherId()).orElseThrow(PublisherNotFoundException::new)
                : null;

        Store store = null;
        if (request.storeId() != null) {
            if (!request.storeId().equals(resolveCallerStoreId())) {
                throw new AdminStoreAccessDeniedException();
            }
            store = storeRepository.findById(request.storeId()).orElseThrow(StoreNotFoundException::new);
        }
        UUID createdByUserId = CurrentUserContext.get().orElse(null);

        Book book = new Book(
                request.isbn13(), request.title(), request.description(), category,
                publisher, store, request.language(), request.pageCount(), request.publishedOn(),
                request.listPrice(), request.currency(), request.coverImageUrl(), createdByUserId
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

    // Overwrites an existing book's editable fields, store-scoped to the caller's admin assignment.
    @Transactional
    public void updateBook(UUID bookId, UpdateBookRequest request) {
        Book book = requireManageableBook(bookId, resolveCallerStoreId());

        Category category = request.categoryId() != null
                ? categoryRepository.findById(request.categoryId()).orElseThrow(CategoryNotFoundException::new)
                : null;
        Publisher publisher = request.publisherId() != null
                ? publisherRepository.findById(request.publisherId()).orElseThrow(PublisherNotFoundException::new)
                : null;

        book.update(
                request.title(), request.description(), request.tableOfContents(),
                category, publisher, request.language(), request.pageCount(),
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

    // Writes a BOOK_UPSERTED event to the outbox table in the same transaction as the triggering write.
    private void publishBookUpserted(UUID bookId) {
        try {
            String payload = objectMapper.writeValueAsString(new BookUpsertedEvent(bookId));
            outboxEventRepository.save(new OutboxEvent("Book", bookId, KafkaTopics.BOOK_UPSERTED, payload));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize outbox event payload", e);
        }
    }

    // Sets a book's absolute stock levels, creating the inventory row if none exists yet.
    @Transactional
    public void updateInventory(UUID bookId, UpdateInventoryRequest request) {
        Book book = requireManageableBook(bookId, resolveCallerStoreId());

        Inventory inventory = inventoryRepository.findById(bookId)
                .orElseGet(() -> new Inventory(book, 0, 0));

        inventory.restock(request.qtyOnHand(), request.reorderThreshold());
        inventoryRepository.save(inventory);
    }

    // Creates or updates a book's virtual edition, auto-detecting the file size when possible.
    @Transactional
    public void upsertVirtualEdition(UUID bookId, UpsertVirtualEditionRequest request) {
        Long fileSizeBytes = detectFileSize(request.fileUrl());
        VirtualEdition edition = virtualEditionRepository.findById(bookId).orElse(null);

        if (edition != null) {
            edition.update(request.fileUrl(), request.fileFormat(), fileSizeBytes, request.price(), request.currency());
        } else {
            Book book = bookRepository.findById(bookId).orElseThrow(BookNotFoundException::new);
            edition = new VirtualEdition(
                    book, request.fileUrl(), request.fileFormat(), fileSizeBytes,
                    request.price(), request.currency(), CurrentUserContext.get().orElse(null)
            );
        }

        virtualEditionRepository.save(edition);
    }

    // Auto-detects file size via a HEAD request's Content-Length for a real http(s) URL — best-effort, same degrade-gracefully approach CatalogClient uses for cover images, so any failure just leaves it unset rather than blocking the save.
    private Long detectFileSize(String fileUrl) {
        if (fileUrl == null || !(fileUrl.startsWith("http://") || fileUrl.startsWith("https://"))) {
            return null;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(fileUrl))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .timeout(Duration.ofSeconds(5))
                    .build();
            HttpResponse<Void> response = FILE_SIZE_CLIENT.send(request, HttpResponse.BodyHandlers.discarding());
            String contentLength = response.headers().firstValue("Content-Length").orElse(null);
            return contentLength != null ? Long.parseLong(contentLength) : null;
        } catch (Exception e) {
            log.warn("Could not auto-detect file size for {}", fileUrl, e);
            return null;
        }
    }

    // Deactivates a book's virtual edition, no-oping if none exists.
    @Transactional
    public void deactivateVirtualEdition(UUID bookId) {
        VirtualEdition edition = virtualEditionRepository.findById(bookId).orElse(null);
        if (edition != null) {
            edition.deactivate();
            virtualEditionRepository.save(edition);
        }
    }
}
