package com.readora.catalog.service;

import com.readora.catalog.dto.ReserveStockRequest;
import com.readora.catalog.dto.ReserveStockResponse;
import com.readora.catalog.entity.Book;
import com.readora.catalog.entity.Inventory;
import com.readora.catalog.exception.BookNotFoundException;
import com.readora.catalog.exception.InsufficientStockException;
import com.readora.catalog.repository.BookRepository;
import com.readora.catalog.repository.InventoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Called directly by commerce-service during checkout (internal, gateway-secret protected, not
 * part of the public /api/v1 surface) to atomically check and reserve stock before an order is
 * persisted — mirrors the doc's Flow 2 "reserve stock, price the order" step. Not documented in
 * the public API Reference since it's service-to-service only, same reasoning as mcp-server's
 * tools not being a public REST surface either.
 */
@Service
public class InventoryService {

    private final BookRepository bookRepository;
    private final InventoryRepository inventoryRepository;

    public InventoryService(BookRepository bookRepository, InventoryRepository inventoryRepository) {
        this.bookRepository = bookRepository;
        this.inventoryRepository = inventoryRepository;
    }

    @Transactional
    public ReserveStockResponse reserve(ReserveStockRequest request) {
        List<ReserveStockResponse.Item> results = request.items().stream().map(item -> {
            Book book = bookRepository.findById(item.bookId())
                    .filter(Book::isActive)
                    .orElseThrow(BookNotFoundException::new);

            Inventory inventory = inventoryRepository.findById(item.bookId())
                    .orElseThrow(BookNotFoundException::new);

            if (!inventory.reserve(item.qty())) {
                throw new InsufficientStockException(item.bookId());
            }
            inventoryRepository.save(inventory);

            return new ReserveStockResponse.Item(book.getId(), book.getTitle(), book.getIsbn13(), book.getListPrice());
        }).toList();

        return new ReserveStockResponse(results);
    }
}
