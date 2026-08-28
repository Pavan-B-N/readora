package com.readora.ai.repository;

import com.readora.ai.entity.EmbeddingJobBookLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface EmbeddingJobBookLogRepository extends JpaRepository<EmbeddingJobBookLog, UUID> {

    /** Newest first — a live feed reads top-to-bottom as "most recently embedded". */
    @Query("SELECT l FROM EmbeddingJobBookLog l WHERE l.job.id = :jobId ORDER BY l.processedAt DESC")
    List<EmbeddingJobBookLog> findAllByJobIdOrderByProcessedAtDesc(@Param("jobId") UUID jobId);
}
