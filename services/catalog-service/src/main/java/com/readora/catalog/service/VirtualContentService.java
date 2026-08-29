package com.readora.catalog.service;

import com.readora.catalog.client.CommerceClient;
import com.readora.catalog.entity.VirtualEdition;
import com.readora.catalog.exception.VirtualEditionNotFoundException;
import com.readora.catalog.exception.VirtualEditionNotOwnedException;
import com.readora.catalog.repository.VirtualEditionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.util.UUID;

/**
 * Serves virtual-edition files for in-app reading only — never a public/downloadable link.
 * fileUrl on VirtualEdition is a filename within the local storage directory, not a full path;
 * resolving it here (rather than trusting a caller-supplied path) is what prevents path
 * traversal outside that directory.
 */
@Service
public class VirtualContentService {

    private final VirtualEditionRepository virtualEditionRepository;
    private final CommerceClient commerceClient;
    private final Path storageRoot;

    public VirtualContentService(
            VirtualEditionRepository virtualEditionRepository,
            CommerceClient commerceClient,
            @Value("${app.storage.path}") String storagePath
    ) {
        this.virtualEditionRepository = virtualEditionRepository;
        this.commerceClient = commerceClient;
        this.storageRoot = Path.of(storagePath).toAbsolutePath().normalize();
    }

    @Transactional(readOnly = true)
    public Resource getContent(UUID userId, UUID bookId) {
        if (!isOwned(userId, bookId)) {
            throw new VirtualEditionNotOwnedException();
        }
        return resolveContent(bookId);
    }

    /**
     * Same file resolution as getContent(), but for trusted internal callers (ai-service's reader
     * embedding pipeline) that have already verified ownership themselves via isOwned() — skips
     * the ownership check since there's no end-user request context to check it against here.
     */
    @Transactional(readOnly = true)
    public Resource getContentForInternalUse(UUID bookId) {
        return resolveContent(bookId);
    }

    @Transactional(readOnly = true)
    public boolean isOwned(UUID userId, UUID bookId) {
        boolean hasActiveEdition = virtualEditionRepository.findById(bookId).filter(VirtualEdition::isActive).isPresent();
        return hasActiveEdition && commerceClient.getPurchasedBookIds(userId).contains(bookId);
    }

    private Resource resolveContent(UUID bookId) {
        VirtualEdition edition = virtualEditionRepository.findById(bookId)
                .filter(VirtualEdition::isActive)
                .orElseThrow(VirtualEditionNotFoundException::new);

        // fileUrl is just a filename (e.g. "java-and-spring.pdf") — resolve it strictly inside
        // storageRoot so it can never escape the directory via "../" or an absolute path.
        Path resolved = storageRoot.resolve(edition.getFileUrl()).normalize();
        if (!resolved.startsWith(storageRoot)) {
            throw new VirtualEditionNotFoundException();
        }

        return new FileSystemResource(resolved);
    }
}
