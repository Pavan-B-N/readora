package com.readora.ai.repository;

import com.readora.ai.entity.EmbeddingJob;
import com.readora.ai.entity.EmbeddingJobStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmbeddingJobRepository extends JpaRepository<EmbeddingJob, UUID> {

    boolean existsByStatusIn(Collection<EmbeddingJobStatus> statuses);

    Optional<EmbeddingJob> findFirstByStatusInOrderByQueuedAtDesc(Collection<EmbeddingJobStatus> statuses);

    List<EmbeddingJob> findAllByOrderByQueuedAtDesc(Pageable pageable);
}
