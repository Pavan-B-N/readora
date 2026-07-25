package com.readora.catalog.service;

import com.readora.catalog.dto.CreateAuthorRequest;
import com.readora.catalog.dto.CreateCategoryRequest;
import com.readora.catalog.dto.CreatePublisherRequest;
import com.readora.catalog.dto.IdResponse;
import com.readora.catalog.dto.UpdateAuthorRequest;
import com.readora.catalog.dto.UpdateCategoryRequest;
import com.readora.catalog.entity.Author;
import com.readora.catalog.entity.Category;
import com.readora.catalog.exception.AuthorInUseException;
import com.readora.catalog.exception.AuthorNotFoundException;
import com.readora.catalog.exception.CategoryInUseException;
import com.readora.catalog.exception.CategoryNotFoundException;
import com.readora.catalog.repository.AuthorRepository;
import com.readora.catalog.repository.BookRepository;
import com.readora.catalog.repository.CategoryRepository;
import com.readora.catalog.repository.PublisherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminCatalogServiceTest {

    @Mock private CategoryRepository categoryRepository;
    @Mock private PublisherRepository publisherRepository;
    @Mock private AuthorRepository authorRepository;
    @Mock private BookRepository bookRepository;

    private AdminCatalogService service;

    @BeforeEach
    void setUp() {
        service = new AdminCatalogService(categoryRepository, publisherRepository, authorRepository, bookRepository);
    }

    @Test
    void createCategory_savesAndReturnsId() {
        IdResponse response = service.createCategory(new CreateCategoryRequest("Fiction", "fiction", 1));

        assertThat(response).isNotNull();
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void updateCategory_notFound_throws() {
        when(categoryRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateCategory(UUID.randomUUID(),
                new UpdateCategoryRequest("Fiction", "fiction", 1)))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void updateCategory_found_appliesUpdateAndSaves() {
        Category category = new Category("Old", "old", 1);
        UUID id = UUID.randomUUID();
        when(categoryRepository.findById(id)).thenReturn(Optional.of(category));

        service.updateCategory(id, new UpdateCategoryRequest("New", "new", 2));

        assertThat(category.getName()).isEqualTo("New");
        verify(categoryRepository).save(category);
    }

    @Test
    void deleteCategory_notFound_throws() {
        when(categoryRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteCategory(UUID.randomUUID()))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void deleteCategory_stillReferencedByABook_throwsInUse() {
        Category category = new Category("Fiction", "fiction", 1);
        UUID id = UUID.randomUUID();
        when(categoryRepository.findById(id)).thenReturn(Optional.of(category));
        when(bookRepository.existsByCategoryId(id)).thenReturn(true);

        assertThatThrownBy(() -> service.deleteCategory(id)).isInstanceOf(CategoryInUseException.class);

        verify(categoryRepository, never()).delete(any());
    }

    @Test
    void deleteCategory_notReferenced_deletes() {
        Category category = new Category("Fiction", "fiction", 1);
        UUID id = UUID.randomUUID();
        when(categoryRepository.findById(id)).thenReturn(Optional.of(category));
        when(bookRepository.existsByCategoryId(id)).thenReturn(false);

        service.deleteCategory(id);

        verify(categoryRepository).delete(category);
    }

    @Test
    void createPublisher_savesAndReturnsId() {
        IdResponse response = service.createPublisher(new CreatePublisherRequest("Penguin", "penguin"));

        assertThat(response).isNotNull();
        verify(publisherRepository).save(any());
    }

    @Test
    void createAuthor_savesAndReturnsId() {
        IdResponse response = service.createAuthor(new CreateAuthorRequest("Author Name", "author-name", null, null));

        assertThat(response).isNotNull();
        verify(authorRepository).save(any(Author.class));
    }

    @Test
    void updateAuthor_notFound_throws() {
        when(authorRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateAuthor(UUID.randomUUID(),
                new UpdateAuthorRequest("New Name", "new-name", null, null)))
                .isInstanceOf(AuthorNotFoundException.class);
    }

    @Test
    void updateAuthor_found_appliesUpdateAndSaves() {
        Author author = new Author("Old", "old", null, null);
        UUID id = UUID.randomUUID();
        when(authorRepository.findById(id)).thenReturn(Optional.of(author));

        service.updateAuthor(id, new UpdateAuthorRequest("New", "new", "bio", "photo"));

        assertThat(author.getName()).isEqualTo("New");
        verify(authorRepository).save(author);
    }

    @Test
    void deleteAuthor_stillCreditedOnABook_throwsInUse() {
        Author author = new Author("Name", "slug", null, null);
        UUID id = UUID.randomUUID();
        when(authorRepository.findById(id)).thenReturn(Optional.of(author));
        when(bookRepository.existsByAuthorsId(id)).thenReturn(true);

        assertThatThrownBy(() -> service.deleteAuthor(id)).isInstanceOf(AuthorInUseException.class);

        verify(authorRepository, never()).delete(any());
    }

    @Test
    void deleteAuthor_notCredited_deletes() {
        Author author = new Author("Name", "slug", null, null);
        UUID id = UUID.randomUUID();
        when(authorRepository.findById(id)).thenReturn(Optional.of(author));
        when(bookRepository.existsByAuthorsId(id)).thenReturn(false);

        service.deleteAuthor(id);

        verify(authorRepository).delete(author);
    }
}
