package com.readora.ai.repository;

import com.readora.ai.entity.BookReaderIndex;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BookReaderIndexRepository extends JpaRepository<BookReaderIndex, UUID> {
}
