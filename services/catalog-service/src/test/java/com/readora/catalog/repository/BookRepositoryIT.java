package com.readora.catalog.repository;

import com.readora.catalog.entity.Book;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BookRepositoryIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine");

    @Autowired
    private BookRepository bookRepository;

    private Book newBook(String isbn13, String title) {
        return new Book(isbn13, title, null, null, null, null, null, "en", null, null, new BigDecimal("299.00"), "INR", null, null);
    }

    @Test
    void duplicateIsbn13_violatesTheRealUniqueConstraint() {
        bookRepository.saveAndFlush(newBook("9781234567897", "First Edition"));

        assertThatThrownBy(() -> bookRepository.saveAndFlush(newBook("9781234567897", "A Different Title, Same ISBN")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void existsByIsbn13_reflectsWhatIsActuallyPersisted() {
        assertThat(bookRepository.existsByIsbn13("9780000000000")).isFalse();

        bookRepository.saveAndFlush(newBook("9780000000000", "Some Title"));

        assertThat(bookRepository.existsByIsbn13("9780000000000")).isTrue();
    }
}
