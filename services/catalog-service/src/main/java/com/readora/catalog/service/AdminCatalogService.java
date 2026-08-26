package com.readora.catalog.service;

import com.readora.catalog.dto.CreateAuthorRequest;
import com.readora.catalog.dto.CreateCategoryRequest;
import com.readora.catalog.dto.CreatePublisherRequest;
import com.readora.catalog.dto.IdResponse;
import com.readora.catalog.entity.Author;
import com.readora.catalog.entity.Category;
import com.readora.catalog.entity.Publisher;
import com.readora.catalog.repository.AuthorRepository;
import com.readora.catalog.repository.CategoryRepository;
import com.readora.catalog.repository.PublisherRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Admin creation of the lookup entities a book references — category, publisher, author. */
@Service
public class AdminCatalogService {

    private final CategoryRepository categoryRepository;
    private final PublisherRepository publisherRepository;
    private final AuthorRepository authorRepository;

    public AdminCatalogService(
            CategoryRepository categoryRepository,
            PublisherRepository publisherRepository,
            AuthorRepository authorRepository
    ) {
        this.categoryRepository = categoryRepository;
        this.publisherRepository = publisherRepository;
        this.authorRepository = authorRepository;
    }

    @Transactional
    public IdResponse createCategory(CreateCategoryRequest request) {
        Category category = new Category(request.name(), request.slug(), request.displayOrder());
        categoryRepository.save(category);
        return new IdResponse(category.getId());
    }

    @Transactional
    public IdResponse createPublisher(CreatePublisherRequest request) {
        Publisher publisher = new Publisher(request.name(), request.slug());
        publisherRepository.save(publisher);
        return new IdResponse(publisher.getId());
    }

    @Transactional
    public IdResponse createAuthor(CreateAuthorRequest request) {
        Author author = new Author(request.name(), request.slug(), request.bio());
        authorRepository.save(author);
        return new IdResponse(author.getId());
    }
}
