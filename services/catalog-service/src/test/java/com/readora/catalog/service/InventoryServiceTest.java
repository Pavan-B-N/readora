package com.readora.catalog.service;

import com.readora.catalog.dto.ReserveStockRequest;
import com.readora.catalog.dto.ReserveStockResponse;
import com.readora.catalog.entity.Book;
import com.readora.catalog.entity.Inventory;
import com.readora.catalog.exception.BookNotFoundException;
import com.readora.catalog.exception.InsufficientStockException;
import com.readora.catalog.repository.BookRepository;
import com.readora.catalog.repository.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

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
class InventoryServiceTest {

    @Mock
    private BookRepository bookRepository;
    @Mock
    private InventoryRepository inventoryRepository;

    private InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        inventoryService = new InventoryService(bookRepository, inventoryRepository);
    }

    private Book activeBook(UUID id, String title, String isbn, BigDecimal price) {
        Book book = new Book(isbn, title, null, null, null, null, null, "en", null, null, price, "INR", null, null);
        ReflectionTestUtils.setField(book, "id", id);
        return book;
    }

    @Test
    void reserve_sufficientStock_reservesAndReturnsBookDetails() {
        UUID bookId = UUID.randomUUID();
        Book book = activeBook(bookId, "The Pragmatic Programmer", "9780135957059", new BigDecimal("499.00"));
        Inventory inventory = new Inventory(book, 10, 2);

        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(inventoryRepository.findById(bookId)).thenReturn(Optional.of(inventory));

        ReserveStockResponse response = inventoryService.reserve(
                new ReserveStockRequest(List.of(new ReserveStockRequest.Item(bookId, 3)))
        );

        assertThat(inventory.getQtyReserved()).isEqualTo(3);
        assertThat(inventory.getAvailable()).isEqualTo(7);
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).title()).isEqualTo("The Pragmatic Programmer");
        assertThat(response.items().get(0).unitPrice()).isEqualByComparingTo("499.00");
        verify(inventoryRepository).save(inventory);
    }

    @Test
    void reserve_insufficientStock_throwsAndDoesNotPartiallyReserve() {
        UUID bookId = UUID.randomUUID();
        Book book = activeBook(bookId, "Clean Code", "9780132350884", new BigDecimal("399.00"));
        Inventory inventory = new Inventory(book, 2, 0);

        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(inventoryRepository.findById(bookId)).thenReturn(Optional.of(inventory));

        assertThatThrownBy(() -> inventoryService.reserve(
                new ReserveStockRequest(List.of(new ReserveStockRequest.Item(bookId, 5)))
        )).isInstanceOf(InsufficientStockException.class);

        assertThat(inventory.getQtyReserved()).isZero();
        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void reserve_bookInactive_throwsBookNotFound() {
        UUID bookId = UUID.randomUUID();
        Book inactiveBook = new Book("9780000000000", "Discontinued Title", null, null, null, null, null, "en", null, null, BigDecimal.TEN, "INR", null, null);
        inactiveBook.deactivate();

        when(bookRepository.findById(bookId)).thenReturn(Optional.of(inactiveBook));

        assertThatThrownBy(() -> inventoryService.reserve(
                new ReserveStockRequest(List.of(new ReserveStockRequest.Item(bookId, 1)))
        )).isInstanceOf(BookNotFoundException.class);
    }

    @Test
    void reserve_unknownBook_throwsBookNotFound() {
        UUID bookId = UUID.randomUUID();
        when(bookRepository.findById(bookId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.reserve(
                new ReserveStockRequest(List.of(new ReserveStockRequest.Item(bookId, 1)))
        )).isInstanceOf(BookNotFoundException.class);
    }
}
