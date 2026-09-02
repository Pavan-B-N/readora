package com.readora.catalog.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.readora.catalog.client.UserServiceClient;
import com.readora.catalog.dto.CreateBookRequest;
import com.readora.catalog.dto.UpdateInventoryRequest;
import com.readora.catalog.entity.Book;
import com.readora.catalog.entity.Inventory;
import com.readora.catalog.entity.Store;
import com.readora.catalog.entity.VirtualEdition;
import com.readora.catalog.exception.AdminStoreAccessDeniedException;
import com.readora.catalog.exception.AdminStoreNotAssignedException;
import com.readora.catalog.exception.AuthorNotFoundException;
import com.readora.catalog.exception.BookNotFoundException;
import com.readora.catalog.exception.CategoryNotFoundException;
import com.readora.catalog.exception.IsbnAlreadyExistsException;
import com.readora.catalog.exception.PublisherNotFoundException;
import com.readora.catalog.repository.AuthorRepository;
import com.readora.catalog.repository.BookRepository;
import com.readora.catalog.repository.CategoryRepository;
import com.readora.catalog.repository.InventoryRepository;
import com.readora.catalog.repository.OutboxEventRepository;
import com.readora.catalog.repository.PublisherRepository;
import com.readora.catalog.repository.StoreRepository;
import com.readora.catalog.repository.VirtualEditionRepository;
import com.readora.sharedcore.security.CurrentUserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminBookServiceTest {

    @Mock private BookRepository bookRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private PublisherRepository publisherRepository;
    @Mock private AuthorRepository authorRepository;
    @Mock private StoreRepository storeRepository;
    @Mock private InventoryRepository inventoryRepository;
    @Mock private VirtualEditionRepository virtualEditionRepository;
    @Mock private OutboxEventRepository outboxEventRepository;
    @Mock private UserServiceClient userServiceClient;

    private AdminBookService adminBookService;
    private final UUID callerId = UUID.randomUUID();
    private final UUID storeId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        adminBookService = new AdminBookService(
                bookRepository, categoryRepository, publisherRepository, authorRepository, storeRepository,
                inventoryRepository, virtualEditionRepository, outboxEventRepository, new ObjectMapper(), userServiceClient
        );
        CurrentUserContext.set(callerId, List.of("ADMIN"));
    }

    @AfterEach
    void clearContext() {
        CurrentUserContext.clear();
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Class<?> clazz = target.getClass();
        while (clazz != null) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }

    private static Book bookInStore(UUID storeId) throws Exception {
        Store store = new Store("S", "City", "L1", null, "State", "000000", "IN");
        setField(store, "id", storeId);
        Book book = new Book("9780000000001", "T", "D", null, null, store, "en", 1, null,
                BigDecimal.TEN, "INR", null, null);
        setField(book, "id", UUID.randomUUID());
        return book;
    }

    @Test
    void createBook_duplicateIsbn_throwsWithoutSaving() {
        CreateBookRequest request = createBookRequest(null);
        when(bookRepository.existsByIsbn13("9780000000001")).thenReturn(true);

        assertThatThrownBy(() -> adminBookService.createBook(request))
                .isInstanceOf(IsbnAlreadyExistsException.class);

        verify(bookRepository, never()).save(any());
    }

    @Test
    void createBook_storeIdNotAssignedToCaller_throwsForbidden() {
        UUID otherStoreId = UUID.randomUUID();
        CreateBookRequest request = createBookRequest(otherStoreId);
        when(bookRepository.existsByIsbn13(any())).thenReturn(false);
        when(userServiceClient.getAdminStoreId(callerId)).thenReturn(storeId);

        assertThatThrownBy(() -> adminBookService.createBook(request))
                .isInstanceOf(AdminStoreAccessDeniedException.class);
    }

    @Test
    void createBook_callerHasNoAssignedStore_throws() {
        CreateBookRequest request = createBookRequest(storeId);
        when(bookRepository.existsByIsbn13(any())).thenReturn(false);
        when(userServiceClient.getAdminStoreId(callerId)).thenReturn(null);

        assertThatThrownBy(() -> adminBookService.createBook(request))
                .isInstanceOf(AdminStoreNotAssignedException.class);
    }

    @Test
    void createBook_valid_savesAndPublishesOutboxEvent() {
        CreateBookRequest request = createBookRequest(null);
        when(bookRepository.existsByIsbn13(any())).thenReturn(false);

        adminBookService.createBook(request);

        verify(bookRepository).save(any(Book.class));
        verify(outboxEventRepository).save(any());
    }

    @Test
    void createBook_categoryNotFound_throws() {
        CreateBookRequest request = new CreateBookRequest(
                "9780000000001", "Title", "Description", null, UUID.randomUUID(), null, null,
                List.of(), "en", 100, null, new BigDecimal("299.00"), "INR", null);
        when(bookRepository.existsByIsbn13(any())).thenReturn(false);
        when(categoryRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminBookService.createBook(request)).isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void createBook_publisherNotFound_throws() {
        CreateBookRequest request = new CreateBookRequest(
                "9780000000001", "Title", "Description", null, null, UUID.randomUUID(), null,
                List.of(), "en", 100, null, new BigDecimal("299.00"), "INR", null);
        when(bookRepository.existsByIsbn13(any())).thenReturn(false);
        when(publisherRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminBookService.createBook(request)).isInstanceOf(PublisherNotFoundException.class);
    }

    @Test
    void createBook_authorNotFound_throws() {
        CreateBookRequest request = new CreateBookRequest(
                "9780000000001", "Title", "Description", null, null, null, null,
                List.of(UUID.randomUUID()), "en", 100, null, new BigDecimal("299.00"), "INR", null);
        when(bookRepository.existsByIsbn13(any())).thenReturn(false);
        when(authorRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminBookService.createBook(request)).isInstanceOf(AuthorNotFoundException.class);
    }

    @Test
    void createBook_storeIdMatchesCallersStore_savesWithStore() {
        CreateBookRequest request = createBookRequest(storeId);
        when(bookRepository.existsByIsbn13(any())).thenReturn(false);
        when(userServiceClient.getAdminStoreId(callerId)).thenReturn(storeId);
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(org.mockito.Mockito.mock(Store.class)));

        adminBookService.createBook(request);

        verify(bookRepository).save(any(Book.class));
    }

    @Test
    void createBook_serializationFailure_wrapsInIllegalStateException() throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper failingMapper = org.mockito.Mockito.mock(com.fasterxml.jackson.databind.ObjectMapper.class);
        when(failingMapper.writeValueAsString(any())).thenThrow(new com.fasterxml.jackson.core.JsonProcessingException("boom") { });
        AdminBookService serviceWithFailingMapper = new AdminBookService(
                bookRepository, categoryRepository, publisherRepository, authorRepository, storeRepository,
                inventoryRepository, virtualEditionRepository, outboxEventRepository, failingMapper, userServiceClient
        );
        when(bookRepository.existsByIsbn13(any())).thenReturn(false);

        assertThatThrownBy(() -> serviceWithFailingMapper.createBook(createBookRequest(null)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void updateBook_bookAtAnotherStore_throwsNotFound() throws Exception {
        Book book = bookInStore(UUID.randomUUID());
        when(bookRepository.findById(book.getId())).thenReturn(Optional.of(book));
        when(userServiceClient.getAdminStoreId(callerId)).thenReturn(storeId);

        assertThatThrownBy(() -> adminBookService.updateBook(book.getId(),
                new com.readora.catalog.dto.UpdateBookRequest(
                        "T", "D", null, null, null, null, "en", 1, null, BigDecimal.TEN, "INR", null, true)))
                .isInstanceOf(BookNotFoundException.class);
    }

    @Test
    void updateInventory_noExistingRow_createsOne() throws Exception {
        Book book = bookInStore(storeId);
        when(bookRepository.findById(book.getId())).thenReturn(Optional.of(book));
        when(userServiceClient.getAdminStoreId(callerId)).thenReturn(storeId);
        when(inventoryRepository.findById(book.getId())).thenReturn(Optional.empty());

        adminBookService.updateInventory(book.getId(), new UpdateInventoryRequest(10, 2));

        verify(inventoryRepository).save(any(Inventory.class));
    }

    @Test
    void upsertVirtualEdition_noExistingEdition_createsFromBook() {
        UUID bookId = UUID.randomUUID();
        Book book = org.mockito.Mockito.mock(Book.class);
        when(virtualEditionRepository.findById(bookId)).thenReturn(Optional.empty());
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));

        adminBookService.upsertVirtualEdition(bookId, new com.readora.catalog.dto.UpsertVirtualEditionRequest(
                "s3://bucket/key.epub", com.readora.catalog.entity.VirtualFileFormat.EPUB, null, new BigDecimal("199.00"), "INR"));

        verify(virtualEditionRepository).save(any(VirtualEdition.class));
    }

    @Test
    void upsertVirtualEdition_bookNotFound_throws() {
        UUID bookId = UUID.randomUUID();
        when(virtualEditionRepository.findById(bookId)).thenReturn(Optional.empty());
        when(bookRepository.findById(bookId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminBookService.upsertVirtualEdition(bookId,
                new com.readora.catalog.dto.UpsertVirtualEditionRequest(
                        "s3://bucket/key.epub", com.readora.catalog.entity.VirtualFileFormat.EPUB, null, new BigDecimal("199.00"), "INR")))
                .isInstanceOf(BookNotFoundException.class);
    }

    @Test
    void deactivateVirtualEdition_noExistingEdition_isANoOp() {
        UUID bookId = UUID.randomUUID();
        when(virtualEditionRepository.findById(bookId)).thenReturn(Optional.empty());

        adminBookService.deactivateVirtualEdition(bookId);

        verify(virtualEditionRepository, never()).save(any());
    }

    @Test
    void getBookForEdit_bookAtAnotherStore_throwsNotFound() throws Exception {
        Book book = bookInStore(UUID.randomUUID());
        when(bookRepository.findById(book.getId())).thenReturn(Optional.of(book));
        when(userServiceClient.getAdminStoreId(callerId)).thenReturn(storeId);

        assertThatThrownBy(() -> adminBookService.getBookForEdit(book.getId()))
                .isInstanceOf(BookNotFoundException.class);
    }

    @Test
    void getBookForEdit_withInventoryAndVirtualEdition_mapsBothNestedDtos() throws Exception {
        Book book = bookInStore(storeId);
        when(bookRepository.findById(book.getId())).thenReturn(Optional.of(book));
        when(userServiceClient.getAdminStoreId(callerId)).thenReturn(storeId);
        Inventory inventory = org.mockito.Mockito.mock(Inventory.class);
        when(inventory.getQtyOnHand()).thenReturn(10);
        when(inventory.getQtyReserved()).thenReturn(2);
        when(inventory.getReorderThreshold()).thenReturn(5);
        when(inventoryRepository.findById(book.getId())).thenReturn(Optional.of(inventory));
        VirtualEdition edition = org.mockito.Mockito.mock(VirtualEdition.class);
        when(virtualEditionRepository.findById(book.getId())).thenReturn(Optional.of(edition));

        var response = adminBookService.getBookForEdit(book.getId());

        assertThat(response.inventory().qtyOnHand()).isEqualTo(10);
        assertThat(response.virtualEdition()).isNotNull();
    }

    @Test
    void getBookForEdit_ownStoreNoInventoryOrVirtualEdition_returnsNullNestedDtos() throws Exception {
        Book book = bookInStore(storeId);
        when(bookRepository.findById(book.getId())).thenReturn(Optional.of(book));
        when(userServiceClient.getAdminStoreId(callerId)).thenReturn(storeId);
        when(inventoryRepository.findById(book.getId())).thenReturn(Optional.empty());
        when(virtualEditionRepository.findById(book.getId())).thenReturn(Optional.empty());

        var response = adminBookService.getBookForEdit(book.getId());

        assertThat(response.inventory()).isNull();
        assertThat(response.virtualEdition()).isNull();
    }

    @Test
    void updateBook_ownStore_appliesUpdateAndPublishesOutboxEvent() throws Exception {
        Book book = bookInStore(storeId);
        when(bookRepository.findById(book.getId())).thenReturn(Optional.of(book));
        when(userServiceClient.getAdminStoreId(callerId)).thenReturn(storeId);

        adminBookService.updateBook(book.getId(), new com.readora.catalog.dto.UpdateBookRequest(
                "New Title", "New Desc", null, null, null, null, "en", 1, null, BigDecimal.TEN, "INR", null, false));

        verify(bookRepository).save(book);
        verify(outboxEventRepository).save(any());
        assertThat(book.getTitle()).isEqualTo("New Title");
    }

    @Test
    void updateBook_withAuthorIds_replacesAuthorSet() throws Exception {
        Book book = bookInStore(storeId);
        when(bookRepository.findById(book.getId())).thenReturn(Optional.of(book));
        when(userServiceClient.getAdminStoreId(callerId)).thenReturn(storeId);
        UUID authorId = UUID.randomUUID();
        var author = new com.readora.catalog.entity.Author("Name", "name", null, null);
        when(authorRepository.findById(authorId)).thenReturn(Optional.of(author));

        adminBookService.updateBook(book.getId(), new com.readora.catalog.dto.UpdateBookRequest(
                "T", "D", null, null, null, List.of(authorId), "en", 1, null, BigDecimal.TEN, "INR", null, true));

        assertThat(book.getAuthors()).containsExactly(author);
    }

    @Test
    void upsertVirtualEdition_existingEdition_updatesInPlace() {
        UUID bookId = UUID.randomUUID();
        VirtualEdition existing = org.mockito.Mockito.mock(VirtualEdition.class);
        when(virtualEditionRepository.findById(bookId)).thenReturn(Optional.of(existing));

        adminBookService.upsertVirtualEdition(bookId, new com.readora.catalog.dto.UpsertVirtualEditionRequest(
                "s3://bucket/key.epub", com.readora.catalog.entity.VirtualFileFormat.EPUB, null, new BigDecimal("199.00"), "INR"));

        verify(existing).update("s3://bucket/key.epub", com.readora.catalog.entity.VirtualFileFormat.EPUB, null,
                new BigDecimal("199.00"), "INR");
        verify(virtualEditionRepository).save(existing);
        verify(bookRepository, never()).findById(any());
    }

    @Test
    void deactivateVirtualEdition_existingEdition_deactivatesAndSaves() {
        UUID bookId = UUID.randomUUID();
        VirtualEdition existing = org.mockito.Mockito.mock(VirtualEdition.class);
        when(virtualEditionRepository.findById(bookId)).thenReturn(Optional.of(existing));

        adminBookService.deactivateVirtualEdition(bookId);

        verify(existing).deactivate();
        verify(virtualEditionRepository).save(existing);
    }

    private static CreateBookRequest createBookRequest(UUID storeId) {
        return new CreateBookRequest(
                "9780000000001", "Title", "Description", null, null, null, storeId,
                List.of(), "en", 100, null, new BigDecimal("299.00"), "INR", null
        );
    }
}
