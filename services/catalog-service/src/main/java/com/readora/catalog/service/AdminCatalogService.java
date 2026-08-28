package com.readora.catalog.service;

import com.readora.catalog.dto.CreateAuthorRequest;
import com.readora.catalog.dto.CreateCategoryRequest;
import com.readora.catalog.dto.CreatePublisherRequest;
import com.readora.catalog.dto.IdResponse;
import com.readora.catalog.dto.UpdateAuthorRequest;
import com.readora.catalog.dto.UpdateCategoryRequest;
import com.readora.catalog.entity.Author;
import com.readora.catalog.entity.Category;
import com.readora.catalog.entity.Publisher;
import com.readora.catalog.exception.AuthorInUseException;
import com.readora.catalog.exception.AuthorNotFoundException;
import com.readora.catalog.exception.CategoryInUseException;
import com.readora.catalog.exception.CategoryNotFoundException;
import com.readora.catalog.repository.AuthorRepository;
import com.readora.catalog.repository.BookRepository;
import com.readora.catalog.repository.CategoryRepository;
import com.readora.catalog.repository.PublisherRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Admin creation of the lookup entities a book references — category, publisher, author. */
@Service
public class AdminCatalogService {

    private final CategoryRepository categoryRepository;
    private final PublisherRepository publisherRepository;
    private final AuthorRepository authorRepository;
    private final BookRepository bookRepository;

    public AdminCatalogService(
            CategoryRepository categoryRepository,
            PublisherRepository publisherRepository,
            AuthorRepository authorRepository,
            BookRepository bookRepository
    ) {
        this.categoryRepository = categoryRepository;
        this.publisherRepository = publisherRepository;
        this.authorRepository = authorRepository;
        this.bookRepository = bookRepository;
    }

    @Transactional
    public IdResponse createCategory(CreateCategoryRequest request) {
        Category category = new Category(request.name(), request.slug(), request.displayOrder());
        categoryRepository.save(category);
        return new IdResponse(category.getId());
    }

    @Transactional
    public void updateCategory(UUID id, UpdateCategoryRequest request) {
        Category category = categoryRepository.findById(id).orElseThrow(CategoryNotFoundException::new);
        category.update(request.name(), request.slug(), request.displayOrder());
        categoryRepository.save(category);
    }

    /** Blocked while any book still references it — reassign those books' category first rather than leaving them dangling. */
    @Transactional
    public void deleteCategory(UUID id) {
        Category category = categoryRepository.findById(id).orElseThrow(CategoryNotFoundException::new);
        if (bookRepository.existsByCategoryId(id)) {
            throw new CategoryInUseException();
        }
        categoryRepository.delete(category);
    }

    @Transactional
    public IdResponse createPublisher(CreatePublisherRequest request) {
        Publisher publisher = new Publisher(request.name(), request.slug());
        publisherRepository.save(publisher);
        return new IdResponse(publisher.getId());
    }

    @Transactional
    public IdResponse createAuthor(CreateAuthorRequest request) {
        Author author = new Author(request.name(), request.slug(), request.bio(), request.photoUrl());
        authorRepository.save(author);
        return new IdResponse(author.getId());
    }

    @Transactional
    public void updateAuthor(UUID id, UpdateAuthorRequest request) {
        Author author = authorRepository.findById(id).orElseThrow(AuthorNotFoundException::new);
        author.update(request.name(), request.slug(), request.bio(), request.photoUrl());
        authorRepository.save(author);
    }

    /** Blocked while any book still credits this author — remove them from those books first rather than leaving a dangling credit. */
    @Transactional
    public void deleteAuthor(UUID id) {
        Author author = authorRepository.findById(id).orElseThrow(AuthorNotFoundException::new);
        if (bookRepository.existsByAuthorsId(id)) {
            throw new AuthorInUseException();
        }
        authorRepository.delete(author);
    }
}
