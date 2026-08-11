package com.readora.ai.repository;

import com.readora.ai.entity.EmbeddingJob;
import com.readora.ai.entity.EmbeddingJobStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// CRUD and lookup access for embedding backfill jobs.
public interface EmbeddingJobRepository extends JpaRepository<EmbeddingJob, UUID> {

    // Whether any job is currently in one of the given statuses.
    boolean existsByStatusIn(Collection<EmbeddingJobStatus> statuses);

    // Finds the most recently queued job among the given statuses.
    Optional<EmbeddingJob> findFirstByStatusInOrderByQueuedAtDesc(Collection<EmbeddingJobStatus> statuses);

    // Pages all jobs, most recently queued first.
    List<EmbeddingJob> findAllByOrderByQueuedAtDesc(Pageable pageable);
}
