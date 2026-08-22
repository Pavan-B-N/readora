package com.readora.catalog.service;

import com.readora.catalog.dto.CreateBookRequest;
import com.readora.catalog.dto.IdResponse;
import com.readora.catalog.dto.UpdateBookRequest;
import com.readora.catalog.dto.UpdateInventoryRequest;
import com.readora.catalog.dto.UpsertVirtualEditionRequest;
import com.readora.catalog.entity.Author;
import com.readora.catalog.entity.Book;
import com.readora.catalog.entity.Category;
import com.readora.catalog.entity.Inventory;
import com.readora.catalog.entity.Publisher;
import com.readora.catalog.entity.VirtualEdition;
import com.readora.catalog.exception.AuthorNotFoundException;
import com.readora.catalog.exception.BookNotFoundException;
import com.readora.catalog.exception.CategoryNotFoundException;
import com.readora.catalog.exception.PublisherNotFoundException;
import com.readora.catalog.repository.AuthorRepository;
import com.readora.catalog.repository.BookRepository;
import com.readora.catalog.repository.CategoryRepository;
import com.readora.catalog.repository.InventoryRepository;
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

    public AdminBookService(
            BookRepository bookRepository,
            CategoryRepository categoryRepository,
            PublisherRepository publisherRepository,
            AuthorRepository authorRepository,
            InventoryRepository inventoryRepository,
            VirtualEditionRepository virtualEditionRepository
    ) {
        this.bookRepository = bookRepository;
        this.categoryRepository = categoryRepository;
        this.publisherRepository = publisherRepository;
        this.authorRepository = authorRepository;
        this.inventoryRepository = inventoryRepository;
        this.virtualEditionRepository = virtualEditionRepository;
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
