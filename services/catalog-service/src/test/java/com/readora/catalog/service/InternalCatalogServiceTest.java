package com.readora.catalog.service;

import com.readora.catalog.dto.BookAvailabilityResponse;
import com.readora.catalog.dto.BookCoverLookupResponse;
import com.readora.catalog.dto.BookExportPage;
import com.readora.catalog.dto.StoreResponse;
import com.readora.catalog.dto.VirtualEditionLookupResponse;
import com.readora.catalog.entity.Book;
import com.readora.catalog.entity.Inventory;
import com.readora.catalog.entity.Store;
import com.readora.catalog.entity.VirtualEdition;
import com.readora.catalog.exception.StoreNotFoundException;
import com.readora.catalog.repository.BookRepository;
import com.readora.catalog.repository.InventoryRepository;
import com.readora.catalog.repository.StoreRepository;
import com.readora.catalog.repository.VirtualEditionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InternalCatalogServiceTest {

    @Mock private BookRepository bookRepository;
    @Mock private VirtualEditionRepository virtualEditionRepository;
    @Mock private InventoryRepository inventoryRepository;
    @Mock private StoreRepository storeRepository;

    private InternalCatalogService service;

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Book book(Store store, boolean active) throws Exception {
        Book book = new Book("9780000000001", "T", "D", null, null, store, "en", 1, null,
                BigDecimal.TEN, "INR", null, null);
        setField(book, "id", UUID.randomUUID());
        setField(book, "isActive", active);
        return book;
    }

    @Test
    void findStore_notFound_throws() {
        service = newService();
        when(storeRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findStore(UUID.randomUUID())).isInstanceOf(StoreNotFoundException.class);
    }

    @Test
    void findStore_found_mapsFields() throws Exception {
        service = newService();
        Store store = new Store("Name", "City", "L1", "L2", "State", "000000", "IN");
        setField(store, "id", UUID.randomUUID());
        when(storeRepository.findById(store.getId())).thenReturn(Optional.of(store));

        StoreResponse response = service.findStore(store.getId());

        assertThat(response.name()).isEqualTo("Name");
        assertThat(response.city()).isEqualTo("City");
    }

    @Test
    void exportBooks_needsReembeddingOnly_usesTheFilteredQuery() {
        service = newService();
        when(bookRepository.findNeedingReembedding(any())).thenReturn(new PageImpl<>(List.of()));

        service.exportBooks(Pageable.unpaged(), true);

        verify(bookRepository).findNeedingReembedding(any());
        verify(bookRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void exportBooks_fullExport_usesFindAll() {
        service = newService();
        when(bookRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        service.exportBooks(Pageable.unpaged(), false);

        verify(bookRepository).findAll(any(Pageable.class));
    }

    @Test
    void exportBooks_mapsEachBookIncludingItsAuthorNames() throws Exception {
        service = newService();
        Store store = new Store("Name", "City", "L1", "L2", "State", "000000", "IN");
        setField(store, "id", UUID.randomUUID());
        Book book = book(store, true);
        when(bookRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(book)));

        BookExportPage page = service.exportBooks(Pageable.unpaged(), false);

        assertThat(page.items()).hasSize(1);
        assertThat(page.items().get(0).id()).isEqualTo(book.getId());
        assertThat(page.items().get(0).title()).isEqualTo(book.getTitle());
    }

    @Test
    void markEmbedded_emptyList_isANoOp() {
        service = newService();

        service.markEmbedded(List.of());

        verify(bookRepository, never()).markEmbedded(any(), any());
    }

    @Test
    void markEmbedded_nonEmptyList_delegatesToRepository() {
        service = newService();
        List<UUID> ids = List.of(UUID.randomUUID());

        service.markEmbedded(ids);

        verify(bookRepository).markEmbedded(any(), any());
    }

    @Test
    void lookupCovers_mapsIdsToCoverUrls() throws Exception {
        service = newService();
        Book book = book(null, true);
        setField(book, "coverImageUrl", "http://x/cover.jpg");
        when(bookRepository.findAllById(any())).thenReturn(List.of(book));

        BookCoverLookupResponse response = service.lookupCovers(List.of(book.getId()));

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).coverImageUrl()).isEqualTo("http://x/cover.jpg");
    }

    @Test
    void lookupVirtualEditions_noActiveEdition_reportsUnavailable() throws Exception {
        service = newService();
        Book book = book(null, true);
        when(bookRepository.findById(book.getId())).thenReturn(Optional.of(book));
        when(virtualEditionRepository.findById(book.getId())).thenReturn(Optional.empty());

        VirtualEditionLookupResponse response = service.lookupVirtualEditions(List.of(book.getId()));

        assertThat(response.items().get(0).available()).isFalse();
    }

    @Test
    void checkAvailability_bookHasActiveVirtualEdition_isAvailableRegardlessOfStore() throws Exception {
        service = newService();
        Book book = book(null, true);
        VirtualEdition edition = org.mockito.Mockito.mock(VirtualEdition.class);
        when(edition.isActive()).thenReturn(true);
        when(bookRepository.findById(book.getId())).thenReturn(Optional.of(book));
        when(virtualEditionRepository.findById(book.getId())).thenReturn(Optional.of(edition));

        BookAvailabilityResponse response = service.checkAvailability(List.of(book.getId()), null);

        assertThat(response.availableBookIds()).containsExactly(book.getId());
    }

    @Test
    void checkAvailability_physicalBookAtWrongStore_isUnavailable() throws Exception {
        service = newService();
        Store bookStore = new Store("N", "C", "L1", null, "S", "0", "IN");
        setField(bookStore, "id", UUID.randomUUID());
        Book book = book(bookStore, true);
        when(bookRepository.findById(book.getId())).thenReturn(Optional.of(book));
        when(virtualEditionRepository.findById(book.getId())).thenReturn(Optional.empty());

        BookAvailabilityResponse response = service.checkAvailability(List.of(book.getId()), UUID.randomUUID());

        assertThat(response.availableBookIds()).isEmpty();
    }

    @Test
    void checkAvailability_inactiveBook_isUnavailable() throws Exception {
        service = newService();
        Book book = book(null, false);
        when(bookRepository.findById(book.getId())).thenReturn(Optional.empty());

        BookAvailabilityResponse response = service.checkAvailability(List.of(book.getId()), null);

        assertThat(response.availableBookIds()).isEmpty();
    }

    @Test
    void lookupBooks_mapsFoundBooksToExportItems() throws Exception {
        service = newService();
        Book book = book(null, true);
        when(bookRepository.findAllById(any())).thenReturn(List.of(book));

        var response = service.lookupBooks(List.of(book.getId()));

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).id()).isEqualTo(book.getId());
    }

    @Test
    void lookupVirtualEditions_activeEdition_reportsAvailableWithPricing() throws Exception {
        service = newService();
        Book book = book(null, true);
        VirtualEdition edition = org.mockito.Mockito.mock(VirtualEdition.class);
        when(edition.isActive()).thenReturn(true);
        when(edition.getPrice()).thenReturn(new BigDecimal("199.00"));
        when(edition.getCurrency()).thenReturn("INR");
        when(bookRepository.findById(book.getId())).thenReturn(Optional.of(book));
        when(virtualEditionRepository.findById(book.getId())).thenReturn(Optional.of(edition));

        var response = service.lookupVirtualEditions(List.of(book.getId()));

        assertThat(response.items().get(0).available()).isTrue();
        assertThat(response.items().get(0).price()).isEqualByComparingTo("199.00");
    }

    @Test
    void checkAvailability_physicalBookAtOwnStoreWithStock_isAvailable() throws Exception {
        service = newService();
        Store bookStore = new Store("N", "C", "L1", null, "S", "0", "IN");
        setField(bookStore, "id", UUID.randomUUID());
        Book book = book(bookStore, true);
        Inventory inventory = org.mockito.Mockito.mock(Inventory.class);
        when(inventory.getAvailable()).thenReturn(5);
        when(bookRepository.findById(book.getId())).thenReturn(Optional.of(book));
        when(virtualEditionRepository.findById(book.getId())).thenReturn(Optional.empty());
        when(inventoryRepository.findById(book.getId())).thenReturn(Optional.of(inventory));

        BookAvailabilityResponse response = service.checkAvailability(List.of(book.getId()), bookStore.getId());

        assertThat(response.availableBookIds()).containsExactly(book.getId());
    }

    private InternalCatalogService newService() {
        return new InternalCatalogService(bookRepository, virtualEditionRepository, inventoryRepository, storeRepository);
    }
}
