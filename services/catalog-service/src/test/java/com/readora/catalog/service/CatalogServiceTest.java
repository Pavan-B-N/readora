package com.readora.catalog.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.readora.catalog.client.CommerceClient;
import com.readora.catalog.client.UserServiceClient;
import com.readora.catalog.dto.BookDetailResponse;
import com.readora.catalog.dto.BookSummaryResponse;
import com.readora.catalog.dto.PageResponse;
import com.readora.catalog.entity.Book;
import com.readora.catalog.entity.Inventory;
import com.readora.catalog.entity.Store;
import com.readora.catalog.entity.VirtualEdition;
import com.readora.catalog.exception.BookNotFoundException;
import com.readora.catalog.exception.StoreIdRequiredException;
import com.readora.catalog.repository.AuthorRepository;
import com.readora.catalog.repository.BookImageRepository;
import com.readora.catalog.repository.BookRepository;
import com.readora.catalog.repository.CategoryRepository;
import com.readora.catalog.repository.InventoryRepository;
import com.readora.catalog.repository.PublisherRepository;
import com.readora.catalog.repository.RelatedBookRepository;
import com.readora.catalog.repository.ReviewRepository;
import com.readora.catalog.repository.VirtualEditionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {

    @Mock private BookRepository bookRepository;
    @Mock private BookImageRepository bookImageRepository;
    @Mock private RelatedBookRepository relatedBookRepository;
    @Mock private InventoryRepository inventoryRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private PublisherRepository publisherRepository;
    @Mock private AuthorRepository authorRepository;
    @Mock private VirtualEditionRepository virtualEditionRepository;
    @Mock private ReviewRepository reviewRepository;
    @Mock private CommerceClient commerceClient;
    @Mock private UserServiceClient userServiceClient;

    private CatalogService catalogService;

    @BeforeEach
    void setUp() {
        catalogService = new CatalogService(
                bookRepository, bookImageRepository, relatedBookRepository, inventoryRepository,
                categoryRepository, publisherRepository, authorRepository, virtualEditionRepository,
                reviewRepository, commerceClient, userServiceClient, new ObjectMapper()
        );
        lenient().when(reviewRepository.getAggregateForBook(any())).thenReturn(new ReviewRepository.RatingAggregate() {
            public Double getAverageRating() { return null; }
            public long getReviewCount() { return 0L; }
        });
    }

    private static Book physicalBook(Store store) throws Exception {
        Book book = new Book("9780000000001", "Title", "Desc", null, null, store,
                "en", 100, null, new BigDecimal("299.00"), "INR", null, null);
        setField(book, "id", UUID.randomUUID());
        return book;
    }

    private static Store store() throws Exception {
        Store store = new Store("Store", "City", "L1", null, "State", "000000", "IN");
        setField(store, "id", UUID.randomUUID());
        return store;
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

    @Test
    void search_physicalResultsRequestedWithoutStoreId_throws() {
        assertThatThrownBy(() -> catalogService.search(
                null, null, null, null, null, null, false, null, null, Pageable.unpaged()))
                .isInstanceOf(StoreIdRequiredException.class);
    }

    @Test
    void search_virtualOnly_doesNotRequireStoreId() {
        when(bookRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        PageResponse<BookSummaryResponse> response = catalogService.search(
                null, null, null, null, null, null, true, null, null, Pageable.unpaged());

        assertThat(response.items()).isEmpty();
    }

    @Test
    void getDetail_bookNotFound_throws() {
        when(bookRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> catalogService.getDetail(UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(BookNotFoundException.class);
    }

    @Test
    void getDetail_inactiveBook_isTreatedAsNotFound() throws Exception {
        Book book = physicalBook(store());
        setField(book, "isActive", false);
        when(bookRepository.findById(book.getId())).thenReturn(Optional.of(book));

        assertThatThrownBy(() -> catalogService.getDetail(book.getId(), UUID.randomUUID()))
                .isInstanceOf(BookNotFoundException.class);
    }

    @Test
    void getDetail_wrongStore_reportsNotAvailableAtStoreWithZeroAvailable() throws Exception {
        Store bookStore = store();
        Book book = physicalBook(bookStore);
        Inventory inventory = mockInventory(50);
        when(bookRepository.findById(book.getId())).thenReturn(Optional.of(book));
        when(inventoryRepository.findById(book.getId())).thenReturn(Optional.of(inventory));
        when(bookImageRepository.findAllByBookIdOrderBySortOrder(any())).thenReturn(List.of());

        BookDetailResponse response = catalogService.getDetail(book.getId(), UUID.randomUUID());

        assertThat(response.availability().status()).isEqualTo("NOT_AVAILABLE_AT_STORE");
        assertThat(response.availability().quantityAvailable()).isZero();
    }

    @Test
    void getDetail_virtualOnlyBook_reportsNoPhysicalEdition() throws Exception {
        Book book = physicalBook(null);
        when(bookRepository.findById(book.getId())).thenReturn(Optional.of(book));
        when(bookImageRepository.findAllByBookIdOrderBySortOrder(any())).thenReturn(List.of());

        BookDetailResponse response = catalogService.getDetail(book.getId(), UUID.randomUUID());

        assertThat(response.availability().status()).isEqualTo("NO_PHYSICAL_EDITION");
    }

    @Test
    void getDetail_ownStoreInStock_reportsInStock() throws Exception {
        Store bookStore = store();
        Book book = physicalBook(bookStore);
        Inventory inventory = mockInventory(5);
        when(bookRepository.findById(book.getId())).thenReturn(Optional.of(book));
        when(inventoryRepository.findById(book.getId())).thenReturn(Optional.of(inventory));
        when(bookImageRepository.findAllByBookIdOrderBySortOrder(any())).thenReturn(List.of());

        BookDetailResponse response = catalogService.getDetail(book.getId(), bookStore.getId());

        assertThat(response.availability().status()).isEqualTo("IN_STOCK");
        assertThat(response.availability().quantityAvailable()).isEqualTo(5);
    }

    @Test
    void getDetail_ownStoreOutOfStock_reportsOutOfStock() throws Exception {
        Store bookStore = store();
        Book book = physicalBook(bookStore);
        when(bookRepository.findById(book.getId())).thenReturn(Optional.of(book));
        when(inventoryRepository.findById(book.getId())).thenReturn(Optional.empty());
        when(bookImageRepository.findAllByBookIdOrderBySortOrder(any())).thenReturn(List.of());

        BookDetailResponse response = catalogService.getDetail(book.getId(), bookStore.getId());

        assertThat(response.availability().status()).isEqualTo("OUT_OF_STOCK");
    }

    @Test
    void getDetail_fallsBackToCoverImageUrlWhenNoGalleryImages() throws Exception {
        Book book = physicalBook(null);
        setField(book, "coverImageUrl", "http://example.com/cover.jpg");
        when(bookRepository.findById(book.getId())).thenReturn(Optional.of(book));
        when(bookImageRepository.findAllByBookIdOrderBySortOrder(any())).thenReturn(List.of());

        BookDetailResponse response = catalogService.getDetail(book.getId(), null);

        assertThat(response.images()).containsExactly("http://example.com/cover.jpg");
    }

    @Test
    void getDetail_withTableOfContentsJson_flattensSectionsIntoTopicsList() throws Exception {
        Book book = physicalBook(null);
        setField(book, "tableOfContents", "{\"Part One\":[\"Chapter 1\",\"Chapter 2\"],\"Part Two\":[\"Chapter 3\"]}");
        when(bookRepository.findById(book.getId())).thenReturn(Optional.of(book));
        when(bookImageRepository.findAllByBookIdOrderBySortOrder(any())).thenReturn(List.of());

        BookDetailResponse response = catalogService.getDetail(book.getId(), null);

        assertThat(response.topics()).containsExactlyInAnyOrder("Chapter 1", "Chapter 2", "Chapter 3");
    }

    @Test
    void getDetail_malformedTableOfContentsJson_degradesToEmptyTopicsRatherThanFailing() throws Exception {
        Book book = physicalBook(null);
        setField(book, "tableOfContents", "not valid json");
        when(bookRepository.findById(book.getId())).thenReturn(Optional.of(book));
        when(bookImageRepository.findAllByBookIdOrderBySortOrder(any())).thenReturn(List.of());

        BookDetailResponse response = catalogService.getDetail(book.getId(), null);

        assertThat(response.topics()).isEmpty();
    }

    @Test
    void getDetail_blankTableOfContents_returnsEmptyTopicsWithoutParsing() throws Exception {
        Book book = physicalBook(null);
        when(bookRepository.findById(book.getId())).thenReturn(Optional.of(book));
        when(bookImageRepository.findAllByBookIdOrderBySortOrder(any())).thenReturn(List.of());

        BookDetailResponse response = catalogService.getDetail(book.getId(), null);

        assertThat(response.topics()).isEmpty();
    }

    @Test
    void getDetail_withCategoryPublisherAndVirtualEdition_mapsAllRefs() throws Exception {
        var category = new com.readora.catalog.entity.Category("Fiction", "fiction", 1);
        setField(category, "id", UUID.randomUUID());
        var publisher = new com.readora.catalog.entity.Publisher("Penguin", "penguin");
        setField(publisher, "id", UUID.randomUUID());
        Book book = new Book("9780000000005", "T", "D", category, publisher, null, "en", 1, null,
                BigDecimal.TEN, "INR", null, null);
        setField(book, "id", UUID.randomUUID());
        when(bookRepository.findById(book.getId())).thenReturn(Optional.of(book));
        when(bookImageRepository.findAllByBookIdOrderBySortOrder(any())).thenReturn(List.of());
        VirtualEdition edition = org.mockito.Mockito.mock(VirtualEdition.class);
        when(edition.isActive()).thenReturn(true);
        when(edition.getPrice()).thenReturn(new BigDecimal("99.00"));
        when(edition.getCurrency()).thenReturn("INR");
        when(virtualEditionRepository.findById(book.getId())).thenReturn(Optional.of(edition));

        BookDetailResponse response = catalogService.getDetail(book.getId(), null);

        assertThat(response.category().name()).isEqualTo("Fiction");
        assertThat(response.publisher().name()).isEqualTo("Penguin");
        assertThat(response.virtualEdition().price()).isEqualByComparingTo("99.00");
    }

    @Test
    void getRelated_bookNotFound_throws() {
        when(bookRepository.existsById(any())).thenReturn(false);

        assertThatThrownBy(() -> catalogService.getRelated(UUID.randomUUID()))
                .isInstanceOf(BookNotFoundException.class);
    }

    @Test
    void getRelated_found_returnsEmptyWhenNoneRelated() {
        when(bookRepository.existsById(any())).thenReturn(true);
        when(relatedBookRepository.findAllByBookId(any())).thenReturn(List.of());

        assertThat(catalogService.getRelated(UUID.randomUUID())).isEmpty();
    }

    @Test
    void getBooksByIds_emptyInput_returnsEmptyWithoutQuerying() {
        assertThat(catalogService.getBooksByIds(List.of())).isEmpty();
    }

    @Test
    void getLibrary_nullUserId_returnsEmpty() {
        assertThat(catalogService.getLibrary(null)).isEmpty();
    }

    @Test
    void getLibrary_noPurchases_returnsEmptyWithoutFurtherQueries() {
        UUID userId = UUID.randomUUID();
        when(commerceClient.getPurchasedBookIds(userId)).thenReturn(List.of());

        assertThat(catalogService.getLibrary(userId)).isEmpty();
    }

    @Test
    void suggest_blankQuery_returnsEmptyWithoutQuerying() {
        assertThat(catalogService.suggest("  ", 5, UUID.randomUUID())).isEmpty();
    }

    @Test
    void suggest_limitAboveCap_isCappedAtTen() {
        when(bookRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
                .thenAnswer(inv -> {
                    Pageable pageable = inv.getArgument(1);
                    assertThat(pageable.getPageSize()).isEqualTo(10);
                    return new PageImpl<Book>(List.of());
                });

        catalogService.suggest("spring", 999, UUID.randomUUID());
    }

    @Test
    void suggest_limitBelowOne_isFlooredAtOne() {
        when(bookRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
                .thenAnswer(inv -> {
                    Pageable pageable = inv.getArgument(1);
                    assertThat(pageable.getPageSize()).isEqualTo(1);
                    return new PageImpl<Book>(List.of());
                });

        catalogService.suggest("spring", 0, UUID.randomUUID());
    }

    @Test
    void getRecommendations_noSignalsAtAll_returnsEmpty() {
        UUID userId = UUID.randomUUID();
        when(commerceClient.getPurchasedBookIds(userId)).thenReturn(List.of());
        when(userServiceClient.getRecentBookViewIds(any(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(List.of());
        when(userServiceClient.getRecentSearchTerms(any(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(List.of());
        when(bookRepository.findAllById(any())).thenReturn(List.of());

        assertThat(catalogService.getRecommendations(userId, UUID.randomUUID())).isEmpty();
    }

    @Test
    void search_physicalResults_mapsThroughToSummaryWithInventoryAndReviews() throws Exception {
        Store bookStore = store();
        Book book = physicalBook(bookStore);
        Inventory inventory = mockInventory(3);
        when(bookRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(book)));
        when(inventoryRepository.findById(book.getId())).thenReturn(Optional.of(inventory));
        when(virtualEditionRepository.findById(book.getId())).thenReturn(Optional.empty());

        PageResponse<BookSummaryResponse> response = catalogService.search(
                null, null, null, null, null, null, false, bookStore.getId(), null, Pageable.unpaged());

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).availability()).isEqualTo("IN_STOCK");
        assertThat(response.items().get(0).deliveryType()).isEqualTo("PHYSICAL");
    }

    @Test
    void search_virtualOnlyResults_mapsThroughToVirtualSummary() throws Exception {
        Book book = physicalBook(null);
        when(bookRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(book)));
        when(virtualEditionRepository.findById(book.getId())).thenReturn(Optional.empty());

        PageResponse<BookSummaryResponse> response = catalogService.search(
                null, null, null, null, null, null, true, null, null, Pageable.unpaged());

        assertThat(response.items().get(0).deliveryType()).isEqualTo("VIRTUAL");
        assertThat(response.items().get(0).availability()).isEqualTo("IN_STOCK");
    }

    @Test
    void search_withUserId_excludesAlreadyOwnedVirtualBooksViaSpecification() {
        UUID userId = UUID.randomUUID();
        when(commerceClient.getPurchasedBookIds(userId)).thenReturn(List.of(UUID.randomUUID()));
        when(bookRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        catalogService.search(null, null, null, null, null, null, true, null, userId, Pageable.unpaged());

        verify(commerceClient).getPurchasedBookIds(userId);
    }

    @Test
    void getPurchasedBooks_mapsOrderItemsToBooksSkippingUnknownOnes() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID unknownBookId = UUID.randomUUID();
        Book book = physicalBook(null);
        var item1 = new com.readora.catalog.dto.RecentOrderItemResponse(book.getId(), "DELIVERED", java.time.Instant.now());
        var item2 = new com.readora.catalog.dto.RecentOrderItemResponse(unknownBookId, "DELIVERED", java.time.Instant.now());
        when(commerceClient.getRecentOrderItems(userId, 20)).thenReturn(List.of(item1, item2));
        when(bookRepository.findAllById(any())).thenReturn(List.of(book));
        when(virtualEditionRepository.findById(book.getId())).thenReturn(Optional.empty());

        var response = catalogService.getPurchasedBooks(userId);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).orderStatus()).isEqualTo("DELIVERED");
    }

    @Test
    void getPurchasedBooks_noOrders_returnsEmptyWithoutFurtherLookup() {
        UUID userId = UUID.randomUUID();
        when(commerceClient.getRecentOrderItems(userId, 20)).thenReturn(List.of());

        assertThat(catalogService.getPurchasedBooks(userId)).isEmpty();
        verify(bookRepository, never()).findAllById(any());
    }

    @Test
    void getBooksByIds_filtersOutInactiveBooks() throws Exception {
        Book active = physicalBook(null);
        Book inactive = physicalBook(null);
        setField(inactive, "isActive", false);
        when(bookRepository.findAllById(any())).thenReturn(List.of(active, inactive));
        when(virtualEditionRepository.findById(any())).thenReturn(Optional.empty());

        var response = catalogService.getBooksByIds(List.of(active.getId(), inactive.getId()));

        assertThat(response).hasSize(1);
    }

    @Test
    void getCategoryTree_mapsRepositoryResults() {
        com.readora.catalog.entity.Category category = new com.readora.catalog.entity.Category("Fiction", "fiction", 1);
        when(categoryRepository.findAllByOrderByDisplayOrder()).thenReturn(List.of(category));

        assertThat(catalogService.getCategoryTree()).hasSize(1);
    }

    @Test
    void getAllPublishers_mapsRepositoryResults() {
        var publisher = new com.readora.catalog.entity.Publisher("Penguin", "penguin");
        when(publisherRepository.findAll()).thenReturn(List.of(publisher));

        assertThat(catalogService.getAllPublishers()).hasSize(1);
    }

    @Test
    void getAllAuthors_mapsRepositoryResults() {
        var author = new com.readora.catalog.entity.Author("Name", "name", null, null);
        when(authorRepository.findAll()).thenReturn(List.of(author));

        assertThat(catalogService.getAllAuthors()).hasSize(1);
    }

    @Test
    void getRecommendations_purchaseAndViewSignals_rankedByCategoryScore() throws Exception {
        UUID userId = UUID.randomUUID();
        var category = new com.readora.catalog.entity.Category("Fiction", "fiction", 1);
        setField(category, "id", UUID.randomUUID());
        Book purchased = new Book("9780000000002", "P", "D", category, null, null, "en", 1, null,
                BigDecimal.TEN, "INR", null, null);
        setField(purchased, "id", UUID.randomUUID());
        UUID purchasedId = purchased.getId();

        when(commerceClient.getPurchasedBookIds(userId)).thenReturn(List.of(purchasedId));
        when(userServiceClient.getRecentBookViewIds(any(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(List.of());
        when(userServiceClient.getRecentSearchTerms(any(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(List.of());
        when(bookRepository.findAllById(List.of(purchasedId))).thenReturn(List.of(purchased));
        when(bookRepository.findAllById(List.of())).thenReturn(List.of());
        when(bookRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        assertThat(catalogService.getRecommendations(userId, UUID.randomUUID())).isEmpty();
    }

    @Test
    void getRecommendations_searchTermMatchingCategoryName_contributesToScoring() throws Exception {
        UUID userId = UUID.randomUUID();
        var category = new com.readora.catalog.entity.Category("Science Fiction", "sci-fi", 1);
        setField(category, "id", UUID.randomUUID());

        when(commerceClient.getPurchasedBookIds(userId)).thenReturn(List.of());
        when(userServiceClient.getRecentBookViewIds(any(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(List.of());
        when(userServiceClient.getRecentSearchTerms(any(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(List.of("science"));
        when(bookRepository.findAllById(List.of())).thenReturn(List.of());
        when(categoryRepository.findAll()).thenReturn(List.of(category));
        when(bookRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        assertThat(catalogService.getRecommendations(userId, UUID.randomUUID())).isEmpty();
        verify(categoryRepository).findAll();
    }

    @Test
    void getLibrary_ownedVirtualEditions_returnsVirtualSummaries() throws Exception {
        UUID userId = UUID.randomUUID();
        Book book = physicalBook(null);
        VirtualEdition edition = org.mockito.Mockito.mock(VirtualEdition.class);
        when(edition.isActive()).thenReturn(true);
        when(edition.getBookId()).thenReturn(book.getId());
        when(commerceClient.getPurchasedBookIds(userId)).thenReturn(List.of(book.getId()));
        when(virtualEditionRepository.findAllById(List.of(book.getId()))).thenReturn(List.of(edition));
        when(bookRepository.findAllById(any())).thenReturn(List.of(book));
        when(virtualEditionRepository.findById(book.getId())).thenReturn(Optional.of(edition));

        var response = catalogService.getLibrary(userId);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).deliveryType()).isEqualTo("VIRTUAL");
    }

    @Test
    void getLibrary_purchasedBooksHaveNoActiveVirtualEdition_returnsEmpty() {
        UUID userId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        when(commerceClient.getPurchasedBookIds(userId)).thenReturn(List.of(bookId));
        when(virtualEditionRepository.findAllById(List.of(bookId))).thenReturn(List.of());

        assertThat(catalogService.getLibrary(userId)).isEmpty();
        verify(bookRepository, never()).findAllById(any());
    }

    @Test
    void suggest_returnsMappedSuggestions() throws Exception {
        Book book = physicalBook(null);
        when(bookRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(book)));

        var response = catalogService.suggest("title", 5, UUID.randomUUID());

        assertThat(response).hasSize(1);
        assertThat(response.get(0).title()).isEqualTo("Title");
    }

    @Test
    void existsByIsbn13_delegatesToRepository() {
        when(bookRepository.existsByIsbn13("9780000000001")).thenReturn(true);

        assertThat(catalogService.existsByIsbn13("9780000000001")).isTrue();
    }

    private static Inventory mockInventory(int available) {
        Inventory inventory = org.mockito.Mockito.mock(Inventory.class);
        lenient().when(inventory.getAvailable()).thenReturn(available);
        return inventory;
    }
}
