package com.readora.ai.repository;

import com.readora.ai.entity.BookReaderIndex;
import com.readora.ai.entity.BookReaderIndexStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Uses the pgvector/pgvector image, not plain postgres — the ai schema's V1 migration runs
 * {@code CREATE EXTENSION vector}, which needs the extension's shared library physically present
 * on the server; a stock postgres:18-alpine image doesn't have it and this migration would fail
 * against one, taking the whole context down with it.
 */
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BookReaderIndexRepositoryIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse("pgvector/pgvector:pg18").asCompatibleSubstituteFor("postgres"));

    @Autowired
    private BookReaderIndexRepository indexRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void markReady_persistsChunkCountAndStatusThroughARealRoundTrip() {
        UUID bookId = UUID.randomUUID();
        BookReaderIndex index = indexRepository.saveAndFlush(new BookReaderIndex(bookId));
        assertThat(index.getStatus()).isEqualTo(BookReaderIndexStatus.PENDING);

        index.markReady(42);
        indexRepository.saveAndFlush(index);
        entityManager.clear();

        BookReaderIndex reloaded = indexRepository.findById(bookId).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(BookReaderIndexStatus.READY);
        assertThat(reloaded.getChunkCount()).isEqualTo(42);
    }
}
